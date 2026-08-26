[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$Version,
    [string]$NbmPath,
    [string]$InstalledUserdir,
    [string]$ExpectedSha256,
    [switch]$RequireOptionalSdkTests,
    [switch]$SkipFreshnessCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ExpectedModuleCodeName = 'dev.flutter.netbeans.netbeans.plugin'
$ExpectedModuleName = 'Flutter and Dart Support'
$ExpectedModuleCategory = 'Flutter'
$CriticalLogPattern = '(?i)SEVERE|Unexpected Exception|LinkageError|NoClassDefFoundError|ClassNotFoundException'
$AuxiliaryConfigurationOrderingLogPattern = (
    '(?i)^\s*WARNING\s+\[org\.openide\.filesystems\.Ordering\]:\s*' +
    'Encountered non-boolean relative ordering attribute\b.*\sfrom\s+' +
    'org\.netbeans\.spi\.project\.AuxiliaryConfiguration\.' +
    'http://www\.netbeans\.org/ns/' +
    '(?:projectui-open-files/2#open-files|' +
    'auxiliary-configuration-preferences/1#preferences|' +
    'editor-bookmarks/2#editor-bookmarks)\s+on\s+'
)
$PluginLayerOrderingLogPattern = (
    '(?i)^\s*WARNING\s+\[org\.openide\.filesystems\.Ordering\]:\s*' +
    '(?:Found same position\b|Not all children\b)' +
    '[^\r\n]*dev-flutter-netbeans-plugin-'
)
$OptionalSdkTestClasses = @(
    'dev.flutter.netbeans.dart.DartAnalysisServerRealSdkTest',
    'dev.flutter.netbeans.dart.DartCandidateAnalyzerRealSdkTest',
    'dev.flutter.netbeans.project.FlutterProjectCreatorRealSdkTest',
    'dev.flutter.netbeans.run.AndroidSdkAvdRealSdkTest',
    'dev.flutter.netbeans.runtime.DartEditorEndToEndIT'
)
$Failures = New-Object 'System.Collections.Generic.List[string]'

function Write-Pass {
    param([string]$Message)
    Write-Output "[PASS] $Message"
}

function Write-Info {
    param([string]$Message)
    Write-Output "[INFO] $Message"
}

function Add-Failure {
    param([string]$Message)
    $Failures.Add($Message)
    Write-Output "[FAIL] $Message"
}

function Get-PluginOwnedOrderingLogLines {
    param([string]$LogText)
    return @($LogText -split "`r?`n" | Where-Object {
            $_ -match $AuxiliaryConfigurationOrderingLogPattern -or
            $_ -match $PluginLayerOrderingLogPattern
        })
}

function Assert-Equal {
    param(
        [AllowNull()]$Actual,
        [AllowNull()]$Expected,
        [string]$Label
    )
    if ([string]$Actual -cne [string]$Expected) {
        Add-Failure "$Label is '$Actual'; expected '$Expected'."
        return
    }
    Write-Pass "$Label = $Expected"
}

function ConvertFrom-SafeXmlText {
    param([string]$Text)

    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
    $settings.XmlResolver = $null
    $stringReader = New-Object System.IO.StringReader -ArgumentList (, $Text)
    $reader = [System.Xml.XmlReader]::Create($stringReader, $settings)
    try {
        $document = New-Object System.Xml.XmlDocument
        $document.XmlResolver = $null
        $document.Load($reader)
        return $document
    } finally {
        $reader.Dispose()
        $stringReader.Dispose()
    }
}

function Read-SafeXmlFile {
    param([string]$Path)
    return ConvertFrom-SafeXmlText ([System.IO.File]::ReadAllText($Path))
}

function Read-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) {
        throw "Archive is missing $EntryName."
    }
    $stream = $entry.Open()
    $reader = New-Object System.IO.StreamReader -ArgumentList $stream
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
        $stream.Dispose()
    }
}

function Get-ManifestAttributes {
    param([string]$Manifest)

    $unfolded = $Manifest -replace "`r?`n ", ''
    $attributes = @{}
    foreach ($line in ($unfolded -split "`r?`n")) {
        if ($line -match '^([^:]+):\s?(.*)$') {
            $attributes[$Matches[1]] = $Matches[2]
        }
    }
    return $attributes
}

function Get-ExpectedSpecificationVersion {
    param([string]$MavenVersion)
    return ($MavenVersion -replace '-SNAPSHOT$', '')
}

function Test-ImplementationVersion {
    param(
        [string]$Actual,
        [string]$MavenVersion,
        [string]$Label
    )

    $specification = Get-ExpectedSpecificationVersion $MavenVersion
    if ($MavenVersion.EndsWith('-SNAPSHOT')) {
        $pattern = '^' + [Regex]::Escape($specification) + '-\d{8}$'
        if ($Actual -notmatch $pattern) {
            Add-Failure "$Label is '$Actual'; expected snapshot pattern '$specification-YYYYMMDD'."
            return
        }
    } elseif ($Actual -cne $specification) {
        Add-Failure "$Label is '$Actual'; expected '$specification'."
        return
    }
    Write-Pass "$Label = $Actual"
}

function Get-AttributeInt {
    param(
        [System.Xml.XmlElement]$Element,
        [string]$Name
    )
    $value = $Element.GetAttribute($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return 0
    }
    return [int]$value
}

function Get-ReportSummary {
    param(
        [System.IO.FileInfo[]]$Reports,
        [string]$Kind
    )

    $summary = [ordered]@{
        Tests = 0
        Failures = 0
        Errors = 0
        Skipped = 0
        SkippedClasses = New-Object 'System.Collections.Generic.List[string]'
    }
    foreach ($report in $Reports) {
        $document = Read-SafeXmlFile $report.FullName
        $root = $document.DocumentElement
        if ($root.LocalName -eq 'testsuite') {
            $suites = @($root)
        } else {
            $suites = @($root.SelectNodes("./*[local-name()='testsuite']"))
        }
        foreach ($suite in $suites) {
            $tests = Get-AttributeInt $suite 'tests'
            $failures = Get-AttributeInt $suite 'failures'
            $errors = Get-AttributeInt $suite 'errors'
            $skipped = Get-AttributeInt $suite 'skipped'
            $summary.Tests += $tests
            $summary.Failures += $failures
            $summary.Errors += $errors
            $summary.Skipped += $skipped
            if ($failures -gt 0 -or $errors -gt 0) {
                Add-Failure "$Kind report '$($report.FullName)' has $failures failure(s) and $errors error(s)."
            }
        }
        foreach ($testcase in @($document.SelectNodes(
                    "//*[local-name()='testcase'][*[local-name()='skipped']]"))) {
            $className = $testcase.GetAttribute('classname')
            if (-not [string]::IsNullOrWhiteSpace($className)) {
                $summary.SkippedClasses.Add($className)
            }
        }
    }
    return [pscustomobject]$summary
}

function Find-TestReport {
    param(
        [System.IO.FileInfo[]]$Reports,
        [string]$ClassName
    )

    foreach ($report in $Reports) {
        if ($report.Name -eq "TEST-$ClassName.xml" -or
                $report.Name.EndsWith(".$ClassName.xml", [StringComparison]::Ordinal)) {
            return $report
        }
    }
    return $null
}

function Verify-TestReports {
    param(
        [string]$Root,
        [System.Xml.XmlDocument]$RootPom
    )

    $unitReports = New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
    $integrationReports = New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
    $unitSources = New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
    $integrationSources = New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
    $moduleNodes = $RootPom.SelectNodes(
        "/*[local-name()='project']/*[local-name()='modules']/*[local-name()='module']")

    foreach ($moduleNode in $moduleNodes) {
        $moduleRoot = Join-Path $Root $moduleNode.InnerText.Trim()
        $testRoot = Join-Path $moduleRoot 'src\test'
        if (Test-Path -LiteralPath $testRoot -PathType Container) {
            foreach ($source in @(Get-ChildItem -LiteralPath $testRoot -Recurse -File -Filter '*.java' |
                    Where-Object { $_.Name -match '(?:Test|IT)\.java$' })) {
                if ($source.Name.EndsWith('IT.java', [StringComparison]::Ordinal)) {
                    $integrationSources.Add($source)
                } else {
                    $unitSources.Add($source)
                }
            }
        }
        $surefireRoot = Join-Path $moduleRoot 'target\surefire-reports'
        if (Test-Path -LiteralPath $surefireRoot -PathType Container) {
            foreach ($report in @(Get-ChildItem -LiteralPath $surefireRoot -File -Filter 'TEST-*.xml')) {
                $unitReports.Add($report)
            }
        }
        $failsafeRoot = Join-Path $moduleRoot 'target\failsafe-reports'
        if (Test-Path -LiteralPath $failsafeRoot -PathType Container) {
            foreach ($report in @(Get-ChildItem -LiteralPath $failsafeRoot -File -Filter 'TEST-*.xml')) {
                $integrationReports.Add($report)
            }
        }
    }

    foreach ($source in $unitSources) {
        $report = Find-TestReport $unitReports.ToArray() $source.BaseName
        if ($null -eq $report) {
            if ($SkipFreshnessCheck) {
                Write-Info "Ignoring newer/unmatched unit test source while freshness checks are disabled: $($source.FullName)"
            } else {
                Add-Failure "No Surefire report exists for test class '$($source.FullName)'."
            }
        } elseif (-not $SkipFreshnessCheck -and
                $report.LastWriteTimeUtc -lt $source.LastWriteTimeUtc) {
            Add-Failure "Surefire report '$($report.FullName)' is older than '$($source.FullName)'."
        }
    }
    foreach ($source in $integrationSources) {
        $report = Find-TestReport $integrationReports.ToArray() $source.BaseName
        if ($null -eq $report) {
            if ($SkipFreshnessCheck) {
                Write-Info "Ignoring newer/unmatched integration test source while freshness checks are disabled: $($source.FullName)"
            } else {
                Add-Failure "No Failsafe report exists for integration test class '$($source.FullName)'."
            }
        } elseif (-not $SkipFreshnessCheck -and
                $report.LastWriteTimeUtc -lt $source.LastWriteTimeUtc) {
            Add-Failure "Failsafe report '$($report.FullName)' is older than '$($source.FullName)'."
        }
    }

    if ($unitReports.Count -eq 0) {
        Add-Failure 'No Surefire XML reports were found.'
    }
    if ($integrationReports.Count -eq 0) {
        Add-Failure 'No Failsafe XML reports were found; run the Maven verify phase.'
    }

    $unit = Get-ReportSummary $unitReports.ToArray() 'Surefire'
    $integration = Get-ReportSummary $integrationReports.ToArray() 'Failsafe'
    Write-Info ("Surefire: tests={0}, failures={1}, errors={2}, skipped={3}, reports={4}" -f
        $unit.Tests, $unit.Failures, $unit.Errors, $unit.Skipped, $unitReports.Count)
    Write-Info ("Failsafe: tests={0}, failures={1}, errors={2}, skipped={3}, reports={4}" -f
        $integration.Tests, $integration.Failures, $integration.Errors,
        $integration.Skipped, $integrationReports.Count)

    if ($unit.Failures + $unit.Errors + $integration.Failures + $integration.Errors -eq 0) {
        Write-Pass 'All recorded Maven tests have zero failures and zero errors.'
    }

    $skippedClasses = @($unit.SkippedClasses) + @($integration.SkippedClasses)
    if ($RequireOptionalSdkTests -and ($unit.Skipped + $integration.Skipped) -gt 0) {
        Add-Failure ("Maven reports contain {0} skipped test(s), but -RequireOptionalSdkTests was set." -f
            ($unit.Skipped + $integration.Skipped))
    } else {
        foreach ($className in $skippedClasses) {
            if ($OptionalSdkTestClasses -notcontains $className) {
                Add-Failure "Unexpected skipped test class '$className'."
            }
        }
        if (($unit.Skipped + $integration.Skipped) -gt $skippedClasses.Count) {
            Add-Failure 'At least one skipped Maven test did not identify its test class.'
        } elseif (($unit.Skipped + $integration.Skipped) -gt 0) {
            Write-Info ('Allowed optional SDK skips: ' +
                (($skippedClasses | Sort-Object -Unique) -join ', '))
        }
    }

    $metadataReport = Find-TestReport $integrationReports.ToArray() 'PluginPackageMetadataIT'
    if ($null -eq $metadataReport) {
        Add-Failure 'PluginPackageMetadataIT Failsafe report is missing.'
    } else {
        Write-Pass "PluginPackageMetadataIT report exists: $($metadataReport.FullName)"
    }
}

function Verify-Nbm {
    param(
        [string]$Path,
        [string]$MavenVersion,
        [string]$Root
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Add-Failure "NBM does not exist: $Path"
        return
    }
    $nbm = Get-Item -LiteralPath $Path
    if ($nbm.Length -le 0) {
        Add-Failure "NBM is empty: $Path"
        return
    }
    Write-Pass "NBM exists: $($nbm.FullName) ($($nbm.Length) bytes)"
    Assert-Equal $nbm.Name "netbeans-plugin-$MavenVersion.nbm" 'NBM file name'

    if (-not $SkipFreshnessCheck) {
        $inputs = New-Object 'System.Collections.Generic.List[System.IO.FileInfo]'
        foreach ($rootFile in @('pom.xml', 'LICENSE')) {
            $candidate = Join-Path $Root $rootFile
            if (Test-Path -LiteralPath $candidate -PathType Leaf) {
                $inputs.Add((Get-Item -LiteralPath $candidate))
            }
        }
        foreach ($moduleNode in $RootPom.SelectNodes(
                "/*[local-name()='project']/*[local-name()='modules']/*[local-name()='module']")) {
            $moduleRoot = Join-Path $Root $moduleNode.InnerText.Trim()
            $modulePom = Join-Path $moduleRoot 'pom.xml'
            if (Test-Path -LiteralPath $modulePom -PathType Leaf) {
                $inputs.Add((Get-Item -LiteralPath $modulePom))
            }
            $mainRoot = Join-Path $moduleRoot 'src\main'
            if (Test-Path -LiteralPath $mainRoot -PathType Container) {
                foreach ($input in @(Get-ChildItem -LiteralPath $mainRoot -Recurse -File)) {
                    $inputs.Add($input)
                }
            }
        }
        $newerInput = $inputs | Where-Object { $_.LastWriteTimeUtc -gt $nbm.LastWriteTimeUtc } |
            Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
        if ($null -ne $newerInput) {
            Add-Failure "NBM is older than release input '$($newerInput.FullName)'. Rebuild it."
        } else {
            Write-Pass 'NBM is not older than any POM, LICENSE, or production source/resource.'
        }
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($nbm.FullName)
    try {
        $info = ConvertFrom-SafeXmlText (Read-ZipEntryText $archive 'Info/info.xml')
        $module = $info.DocumentElement
        $manifest = $module.SelectSingleNode("./*[local-name()='manifest']")
        $license = $module.SelectSingleNode("./*[local-name()='license']")
        if ($null -eq $manifest) {
            Add-Failure 'Info/info.xml is missing its manifest element.'
            return
        }
        if ($null -eq $license) {
            Add-Failure 'Info/info.xml is missing its license element.'
            return
        }

        Assert-Equal $module.GetAttribute('codenamebase') $ExpectedModuleCodeName 'NBM codenamebase'
        Assert-Equal $module.GetAttribute('distribution') $nbm.Name 'NBM distribution'
        Assert-Equal $manifest.GetAttribute('OpenIDE-Module') $ExpectedModuleCodeName 'Module code name'
        Assert-Equal $manifest.GetAttribute('OpenIDE-Module-Name') $ExpectedModuleName 'Module name'
        Assert-Equal $manifest.GetAttribute('OpenIDE-Module-Display-Category') `
            $ExpectedModuleCategory 'Module category'
        Assert-Equal $manifest.GetAttribute('OpenIDE-Module-Specification-Version') `
            (Get-ExpectedSpecificationVersion $MavenVersion) 'Module specification version'
        Test-ImplementationVersion `
            $manifest.GetAttribute('OpenIDE-Module-Implementation-Version') `
            $MavenVersion 'Module implementation version'
        Assert-Equal $license.GetAttribute('name') $module.GetAttribute('license') 'NBM license id'

        $licenseText = ($license.InnerText -replace "`r`n", "`n").TrimEnd()
        if ($licenseText -notmatch 'Apache License' -or $licenseText -notmatch 'Version 2\.0') {
            Add-Failure 'NBM license does not identify Apache License, Version 2.0.'
        } else {
            Write-Pass 'NBM license identifies Apache License, Version 2.0.'
        }
        $sourceLicensePath = Join-Path $Root 'LICENSE'
        if (Test-Path -LiteralPath $sourceLicensePath -PathType Leaf) {
            $sourceLicense = ([System.IO.File]::ReadAllText($sourceLicensePath) `
                    -replace "`r`n", "`n").TrimEnd()
            if (-not $licenseText.Contains($sourceLicense)) {
                Add-Failure 'NBM does not contain the complete repository LICENSE text.'
            } else {
                Write-Pass 'NBM contains the complete repository LICENSE text.'
            }
        }

        foreach ($entryName in @(
                'netbeans/config/Modules/dev-flutter-netbeans-netbeans-plugin.xml',
                'netbeans/modules/dev-flutter-netbeans-netbeans-plugin.jar')) {
            if ($null -eq $archive.GetEntry($entryName)) {
                Add-Failure "NBM is missing $entryName."
            } else {
                Write-Pass "NBM contains $entryName."
            }
        }
    } finally {
        $archive.Dispose()
    }

    $hash = (Get-FileHash -LiteralPath $nbm.FullName -Algorithm SHA256).Hash.ToUpperInvariant()
    Write-Info "SHA256 $hash  $($nbm.FullName)"
    if (-not [string]::IsNullOrWhiteSpace($ExpectedSha256)) {
        $normalizedExpected = ($ExpectedSha256 -replace '\s', '').ToUpperInvariant()
        if ($normalizedExpected -notmatch '^[0-9A-F]{64}$') {
            Add-Failure '-ExpectedSha256 must contain exactly 64 hexadecimal characters.'
        } elseif ($hash -cne $normalizedExpected) {
            Add-Failure "NBM SHA256 is $hash; expected $normalizedExpected."
        } else {
            Write-Pass 'NBM SHA256 matches the expected value.'
        }
    }
}

function Get-TrackingOriginValidation {
    param(
        [System.Xml.XmlElement]$VersionElement,
        [string]$SpecificationVersion,
        [System.IO.FileInfo]$Nbm
    )

    $origin = $VersionElement.GetAttribute('origin')
    if ($null -eq $Nbm) {
        return [pscustomobject]@{
            Valid = $false
            Description = $origin
            Error = 'Cannot validate update tracking origin without the NBM artifact.'
        }
    }
    if ($origin -ceq $Nbm.Name) {
        return [pscustomobject]@{
            Valid = $true
            Description = "direct NBM $origin"
            Error = $null
        }
    }

    try {
        $originUri = [System.Uri]::new($origin)
    } catch {
        return [pscustomobject]@{
            Valid = $false
            Description = $origin
            Error = "Update tracking origin '$origin' is neither the NBM name nor a valid URI."
        }
    }
    if (-not $originUri.IsAbsoluteUri -or -not $originUri.IsFile) {
        return [pscustomobject]@{
            Valid = $false
            Description = $origin
            Error = "Update tracking catalog origin is not an absolute local file URI: $origin"
        }
    }
    $catalogPath = [System.IO.Path]::GetFullPath($originUri.LocalPath)
    if ([System.IO.Path]::GetFileName($catalogPath) -cne 'updates.xml' -or
            -not (Test-Path -LiteralPath $catalogPath -PathType Leaf)) {
        return [pscustomobject]@{
            Valid = $false
            Description = $origin
            Error = "Update tracking catalog does not resolve to an existing updates.xml: $catalogPath"
        }
    }

    try {
        $catalog = Read-SafeXmlFile $catalogPath
        $module = $catalog.SelectSingleNode(
            "/*[local-name()='module_updates']/*[local-name()='module'][@codenamebase='$ExpectedModuleCodeName']")
        if ($null -eq $module) {
            throw "catalog has no $ExpectedModuleCodeName module entry"
        }
        $manifest = $module.SelectSingleNode("./*[local-name()='manifest']")
        if ($null -eq $manifest) {
            throw 'catalog module entry has no manifest'
        }
        if ($manifest.GetAttribute('OpenIDE-Module-Specification-Version') -cne
                $SpecificationVersion) {
            throw ("catalog specification version is '" +
                $manifest.GetAttribute('OpenIDE-Module-Specification-Version') +
                "'; expected '$SpecificationVersion'")
        }
        if ($module.GetAttribute('distribution') -cne $Nbm.Name) {
            throw ("catalog distribution is '$($module.GetAttribute('distribution'))'; " +
                "expected '$($Nbm.Name)'")
        }
        if ($module.GetAttribute('downloadsize') -cne [string]$Nbm.Length) {
            throw ("catalog downloadsize is '$($module.GetAttribute('downloadsize'))'; " +
                "expected '$($Nbm.Length)'")
        }
        $digest = $module.SelectSingleNode(
            "./*[local-name()='message_digest'][translate(@algorithm, 'sha', 'SHA')='SHA-512']")
        if ($null -eq $digest) {
            throw 'catalog module entry has no SHA-512 message_digest'
        }
        $expectedSha512 = (Get-FileHash -LiteralPath $Nbm.FullName -Algorithm SHA512).Hash
        if ($digest.GetAttribute('value').ToUpperInvariant() -cne $expectedSha512) {
            throw 'catalog SHA-512 does not match the verified NBM'
        }
        $stagedNbmPath = Join-Path (Split-Path -Parent $catalogPath) $Nbm.Name
        if (-not (Test-Path -LiteralPath $stagedNbmPath -PathType Leaf)) {
            throw "catalog distribution file does not exist: $stagedNbmPath"
        }
        $stagedSha256 = (Get-FileHash -LiteralPath $stagedNbmPath -Algorithm SHA256).Hash
        $artifactSha256 = (Get-FileHash -LiteralPath $Nbm.FullName -Algorithm SHA256).Hash
        if ($stagedSha256 -cne $artifactSha256) {
            throw 'catalog distribution bytes differ from the verified NBM'
        }
    } catch {
        return [pscustomobject]@{
            Valid = $false
            Description = $origin
            Error = "Invalid tracked update catalog '$catalogPath': $($_.Exception.Message)"
        }
    }
    return [pscustomobject]@{
        Valid = $true
        Description = "verified local catalog $catalogPath"
        Error = $null
    }
}

function Verify-InstalledUserdir {
    param(
        [string]$Userdir,
        [string]$MavenVersion,
        [System.IO.FileInfo]$Nbm
    )

    if ([string]::IsNullOrWhiteSpace($Userdir)) {
        Write-Info 'Installed userdir verification was not requested.'
        return
    }
    if (-not (Test-Path -LiteralPath $Userdir -PathType Container)) {
        Add-Failure "Installed NetBeans userdir does not exist: $Userdir"
        return
    }
    $resolvedUserdir = (Resolve-Path -LiteralPath $Userdir).Path
    $configPath = Join-Path $resolvedUserdir `
        'config\Modules\dev-flutter-netbeans-netbeans-plugin.xml'
    $trackingPath = Join-Path $resolvedUserdir `
        'update_tracking\dev-flutter-netbeans-netbeans-plugin.xml'
    $logPath = Join-Path $resolvedUserdir 'var\log\messages.log'

    foreach ($required in @($configPath, $trackingPath, $logPath)) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
            Add-Failure "Installed userdir is missing '$required'."
        }
    }
    if (-not (Test-Path -LiteralPath $configPath -PathType Leaf) -or
            -not (Test-Path -LiteralPath $trackingPath -PathType Leaf) -or
            -not (Test-Path -LiteralPath $logPath -PathType Leaf)) {
        return
    }

    $config = Read-SafeXmlFile $configPath
    Assert-Equal $config.DocumentElement.GetAttribute('name') $ExpectedModuleCodeName `
        'Installed module code name'
    $parameters = @{}
    foreach ($parameter in @($config.DocumentElement.SelectNodes("./*[local-name()='param']"))) {
        $parameters[$parameter.GetAttribute('name')] = $parameter.InnerText.Trim()
    }
    Assert-Equal $parameters['enabled'] 'true' 'Installed module enabled state'
    if (-not $parameters.ContainsKey('jar')) {
        Add-Failure 'Installed module config does not declare its JAR.'
        return
    }
    $jarPath = Join-Path $resolvedUserdir ($parameters['jar'] -replace '/', '\')
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        Add-Failure "Installed module JAR does not exist: $jarPath"
        return
    }
    Write-Pass "Installed module JAR exists: $jarPath"

    $tracking = Read-SafeXmlFile $trackingPath
    Assert-Equal $tracking.DocumentElement.GetAttribute('codename') $ExpectedModuleCodeName `
        'Update tracking code name'
    $lastVersion = $tracking.SelectSingleNode(
        "/*[local-name()='module']/*[local-name()='module_version'][@last='true']")
    if ($null -eq $lastVersion) {
        Add-Failure 'Update tracking has no last=true module_version.'
    } else {
        Assert-Equal $lastVersion.GetAttribute('specification_version') `
            (Get-ExpectedSpecificationVersion $MavenVersion) 'Tracked specification version'
        $expectedSpecification = Get-ExpectedSpecificationVersion $MavenVersion
        $originValidation = $null
        if ($lastVersion.GetAttribute('origin') -ceq 'updater') {
            $candidateErrors = [System.Collections.Generic.List[string]]::new()
            foreach ($candidateVersion in @($tracking.SelectNodes(
                        "/*[local-name()='module']/*[local-name()='module_version']"))) {
                if ([object]::ReferenceEquals($candidateVersion, $lastVersion) -or
                        $candidateVersion.GetAttribute('specification_version') -cne
                        $expectedSpecification) {
                    continue
                }
                $candidateValidation = Get-TrackingOriginValidation `
                    $candidateVersion $expectedSpecification $Nbm
                if ($candidateValidation.Valid) {
                    $originValidation = $candidateValidation
                    break
                } elseif (-not [string]::IsNullOrWhiteSpace($candidateValidation.Error)) {
                    $candidateErrors.Add($candidateValidation.Error)
                }
            }
            if ($null -eq $originValidation) {
                $details = if ($candidateErrors.Count -eq 0) {
                    ''
                } else {
                    ' ' + (($candidateErrors | Select-Object -Unique) -join ' | ')
                }
                Add-Failure ("Tracked origin is 'updater', but no same-version entry " +
                    "identifies the verified NBM or a matching local update catalog.$details")
            } else {
                Write-Pass ("Tracked updater origin is backed by " +
                    "$($originValidation.Description).")
            }
        } else {
            $originValidation = Get-TrackingOriginValidation `
                $lastVersion $expectedSpecification $Nbm
            if (-not $originValidation.Valid) {
                Add-Failure $originValidation.Error
            } else {
                Write-Pass "Tracked origin is $($originValidation.Description)."
            }
        }
        $trackedFiles = @($lastVersion.SelectNodes("./*[local-name()='file']"))
        $missing = @()
        foreach ($trackedFile in $trackedFiles) {
            $installedPath = Join-Path $resolvedUserdir `
                ($trackedFile.GetAttribute('name') -replace '/', '\')
            if (-not (Test-Path -LiteralPath $installedPath -PathType Leaf)) {
                $missing += $installedPath
            }
        }
        if ($trackedFiles.Count -eq 0) {
            Add-Failure 'Update tracking does not list any installed files.'
        } elseif ($missing.Count -gt 0) {
            Add-Failure ("Update tracking references missing files: " + ($missing -join ', '))
        } else {
            Write-Pass "All $($trackedFiles.Count) update-tracked files exist."
        }
    }

    $jarArchive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $manifest = Get-ManifestAttributes (Read-ZipEntryText $jarArchive 'META-INF/MANIFEST.MF')
        Assert-Equal $manifest['OpenIDE-Module'] $ExpectedModuleCodeName `
            'Installed JAR module code name'
        Assert-Equal $manifest['OpenIDE-Module-Specification-Version'] `
            (Get-ExpectedSpecificationVersion $MavenVersion) `
            'Installed JAR specification version'
        Test-ImplementationVersion $manifest['OpenIDE-Module-Implementation-Version'] `
            $MavenVersion 'Installed JAR implementation version'
    } finally {
        $jarArchive.Dispose()
    }

    $log = [System.IO.File]::ReadAllText($logPath)
    if ($log -notmatch '(?m)^\s*Product Version\s*=.*(?:NetBeans.*30|Build 30-)') {
        Add-Failure 'messages.log does not identify a NetBeans 30 product/build.'
    } else {
        Write-Pass 'messages.log identifies a NetBeans 30 product/build.'
    }
    $criticalLines = @($log -split "`r?`n" | Where-Object { $_ -match $CriticalLogPattern })
    if ($criticalLines.Count -gt 0) {
        Add-Failure ("messages.log contains critical pattern(s): " +
            (($criticalLines | Select-Object -First 5) -join ' | '))
    } else {
        Write-Pass 'messages.log contains no configured critical error patterns.'
    }
    $pluginOrderingEvidence = [System.Collections.Generic.List[string]]::new()
    $logDirectory = Split-Path -Parent $logPath
    $orderingLogPaths = @(Get-ChildItem -LiteralPath $logDirectory `
            -Filter 'messages.log*' -File -ErrorAction SilentlyContinue)
    foreach ($orderingLogPath in $orderingLogPaths) {
        $orderingLog = [System.IO.File]::ReadAllText($orderingLogPath.FullName)
        foreach ($line in @(Get-PluginOwnedOrderingLogLines $orderingLog)) {
            $pluginOrderingEvidence.Add("$($orderingLogPath.Name): $line")
        }
    }
    if ($pluginOrderingEvidence.Count -gt 0) {
        Add-Failure ("NetBeans logs contain plugin-owned Ordering warning(s): " +
            (($pluginOrderingEvidence | Select-Object -First 5) -join ' | '))
    } else {
        Write-Pass 'NetBeans logs contain no plugin-owned Ordering warnings.'
    }

    $escapedCodeName = [Regex]::Escape($ExpectedModuleCodeName)
    $moduleLine = $log -split "`r?`n" |
        Where-Object { $_ -match "^\s*$escapedCodeName\s+\[" } |
        Select-Object -Last 1
    if ($null -eq $moduleLine) {
        Add-Failure 'messages.log does not show the installed module being turned on.'
    } elseif ($moduleLine -notmatch "^\s*$escapedCodeName\s+\[(\S+)\s+(\S+)") {
        Add-Failure "Could not parse the installed module version from: $moduleLine"
    } else {
        Assert-Equal $Matches[1] (Get-ExpectedSpecificationVersion $MavenVersion) `
            'Logged module specification version'
        Test-ImplementationVersion $Matches[2] $MavenVersion `
            'Logged module implementation version'
    }

    if (-not $SkipFreshnessCheck -and $null -ne $Nbm -and
            (Get-Item -LiteralPath $logPath).LastWriteTimeUtc -lt $Nbm.LastWriteTimeUtc) {
        Add-Failure 'Installed userdir log is older than the NBM; repeat the installation smoke.'
    }
}

try {
    $RepositoryRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
    $rootPomPath = Join-Path $RepositoryRoot 'pom.xml'
    if (-not (Test-Path -LiteralPath $rootPomPath -PathType Leaf)) {
        throw "Repository root does not contain pom.xml: $RepositoryRoot"
    }
    $RootPom = Read-SafeXmlFile $rootPomPath
    if ([string]::IsNullOrWhiteSpace($Version)) {
        $versionNode = $RootPom.SelectSingleNode(
            "/*[local-name()='project']/*[local-name()='version']")
        if ($null -eq $versionNode -or [string]::IsNullOrWhiteSpace($versionNode.InnerText)) {
            throw 'Could not read the Maven version from the root pom.xml.'
        }
        $Version = $versionNode.InnerText.Trim()
    }
    if ([string]::IsNullOrWhiteSpace($NbmPath)) {
        $NbmPath = Join-Path $RepositoryRoot `
            "netbeans-plugin\target\netbeans-plugin-$Version.nbm"
    } elseif (-not [System.IO.Path]::IsPathRooted($NbmPath)) {
        $NbmPath = Join-Path $RepositoryRoot $NbmPath
    }

    Write-Info "Repository root: $RepositoryRoot"
    Write-Info "Expected Maven version: $Version"
    Verify-TestReports $RepositoryRoot $RootPom
    Verify-Nbm $NbmPath $Version $RepositoryRoot
    $nbmItem = if (Test-Path -LiteralPath $NbmPath -PathType Leaf) {
        Get-Item -LiteralPath $NbmPath
    } else {
        $null
    }
    Verify-InstalledUserdir $InstalledUserdir $Version $nbmItem
} catch {
    Add-Failure "Unexpected verification error: $($_.Exception.Message)"
}

if ($Failures.Count -gt 0) {
    Write-Output "Release verification FAILED with $($Failures.Count) problem(s)."
    exit 1
}

Write-Output 'Release verification PASSED.'
exit 0
