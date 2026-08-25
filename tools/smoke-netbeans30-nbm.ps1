[CmdletBinding()]
param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$NetBeansHome = $env:NETBEANS_HOME,
    [string]$CurrentNbmPath,
    [string]$PreviousNbmPath,
    [string]$ProbeRoot,
    [ValidateRange(10, 600)]
    [int]$StartupTimeoutSeconds = 90,
    [ValidateRange(10, 600)]
    [int]$CommandTimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ExpectedModuleCodeName = 'dev.flutter.netbeans.netbeans.plugin'
$ModuleConfigName = 'dev-flutter-netbeans-netbeans-plugin.xml'
$FlutterPreferencesRelativePath = 'config\Preferences\dev\flutter\netbeans\netbeans\plugin.properties'
$ProjectUiPreferencesRelativePath = 'config\Preferences\org\netbeans\modules\projectui.properties'
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
$HarmlessCliStderrPatterns = @(
    '^\s*WARNING: package com\.sun\.tools\.classfile not in jdk\.jdeps\s*$',
    '^\s*WARNING: package com\.apple\.laf not in java\.desktop\s*$',
    '^\s*WARNING: package sun\.awt\.X11 not in java\.desktop\s*$',
    '^\s*WARNING: package com\.sun\.java\.swing\.plaf\.gtk not in java\.desktop\s*$'
)

function Write-Pass {
    param([string]$Message)
    Write-Host "[PASS] $Message"
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message"
}

function ConvertFrom-SafeXmlText {
    param([string]$Text)

    $settings = [System.Xml.XmlReaderSettings]::new()
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
    $settings.XmlResolver = $null
    $stringReader = [System.IO.StringReader]::new($Text)
    $reader = [System.Xml.XmlReader]::Create($stringReader, $settings)
    try {
        $document = [System.Xml.XmlDocument]::new()
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

function Read-SharedTextFile {
    param([string]$Path)

    $stream = [System.IO.File]::Open(
        $Path,
        [System.IO.FileMode]::Open,
        [System.IO.FileAccess]::Read,
        [System.IO.FileShare]::ReadWrite)
    $reader = [System.IO.StreamReader]::new($stream)
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
        $stream.Dispose()
    }
}

function ConvertFrom-JavaPropertiesEscape {
    param([string]$Value)

    $result = [System.Text.StringBuilder]::new()
    for ($index = 0; $index -lt $Value.Length; $index++) {
        $character = $Value[$index]
        if ($character -ne '\') {
            [void]$result.Append($character)
            continue
        }
        if (++$index -ge $Value.Length) {
            [void]$result.Append('\')
            break
        }
        $escaped = $Value[$index]
        switch ($escaped) {
            't' { [void]$result.Append("`t") }
            'n' { [void]$result.Append("`n") }
            'r' { [void]$result.Append("`r") }
            'f' { [void]$result.Append("`f") }
            'u' {
                if ($index + 4 -ge $Value.Length) {
                    throw "Invalid Java properties Unicode escape in '$Value'."
                }
                $hex = $Value.Substring($index + 1, 4)
                if ($hex -notmatch '^[0-9A-Fa-f]{4}$') {
                    throw "Invalid Java properties Unicode escape '\u$hex'."
                }
                [void]$result.Append([char][Convert]::ToInt32($hex, 16))
                $index += 4
            }
            default { [void]$result.Append($escaped) }
        }
    }
    return $result.ToString()
}

function Read-JavaPropertiesFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Java properties file does not exist: $Path"
    }
    $properties = @{}
    foreach ($line in ([System.IO.File]::ReadAllLines($Path))) {
        $trimmed = $line.TrimStart()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or
                $trimmed.StartsWith('#') -or $trimmed.StartsWith('!')) {
            continue
        }

        $separator = -1
        $escaped = $false
        for ($index = 0; $index -lt $line.Length; $index++) {
            $character = $line[$index]
            if ($escaped) {
                $escaped = $false
                continue
            }
            if ($character -eq '\') {
                $escaped = $true
                continue
            }
            if ($character -eq '=' -or $character -eq ':' -or
                    [char]::IsWhiteSpace($character)) {
                $separator = $index
                break
            }
        }
        if ($separator -lt 0) {
            $keyText = $line
            $valueText = ''
        } else {
            $keyText = $line.Substring(0, $separator)
            $valueStart = $separator
            while ($valueStart -lt $line.Length -and
                    [char]::IsWhiteSpace($line[$valueStart])) {
                $valueStart++
            }
            if ($valueStart -lt $line.Length -and
                    ($line[$valueStart] -eq '=' -or $line[$valueStart] -eq ':')) {
                $valueStart++
            }
            while ($valueStart -lt $line.Length -and
                    [char]::IsWhiteSpace($line[$valueStart])) {
                $valueStart++
            }
            $valueText = $line.Substring($valueStart)
        }
        $key = ConvertFrom-JavaPropertiesEscape $keyText
        if ($properties.ContainsKey($key)) {
            throw "Java properties file contains duplicate key '$key': $Path"
        }
        $properties[$key] = ConvertFrom-JavaPropertiesEscape $valueText
    }
    return $properties
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
    $reader = [System.IO.StreamReader]::new($stream)
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
        $stream.Dispose()
    }
}

function Get-StreamSha256 {
    param([System.IO.Stream]$Stream)

    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([System.BitConverter]::ToString(
                $sha256.ComputeHash($Stream))).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha256.Dispose()
    }
}

function Get-CanonicalPath {
    param([string]$Path)

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $root = [System.IO.Path]::GetPathRoot($fullPath)
    if ($fullPath.Length -gt $root.Length) {
        $fullPath = $fullPath.TrimEnd(
            [System.IO.Path]::DirectorySeparatorChar,
            [System.IO.Path]::AltDirectorySeparatorChar)
    }
    return $fullPath
}

function Test-SameCanonicalPath {
    param(
        [string]$Left,
        [string]$Right
    )

    return (Get-CanonicalPath $Left).Equals(
        (Get-CanonicalPath $Right),
        [System.StringComparison]::OrdinalIgnoreCase)
}

function Get-ManifestAttributes {
    param([string]$Manifest)

    $attributes = @{}
    $unfolded = $Manifest -replace "`r?`n ", ''
    foreach ($line in ($unfolded -split "`r?`n")) {
        if ($line -match '^([^:]+):\s?(.*)$') {
            $attributes[$Matches[1]] = $Matches[2]
        }
    }
    return $attributes
}

function Get-NbmMetadata {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "NBM does not exist: $Path"
    }
    $nbm = Get-Item -LiteralPath $Path
    if ($nbm.Length -le 0) {
        throw "NBM is empty: $($nbm.FullName)"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($nbm.FullName)
    try {
        $info = ConvertFrom-SafeXmlText (Read-ZipEntryText $archive 'Info/info.xml')
        $module = $info.DocumentElement
        $manifest = $module.SelectSingleNode("./*[local-name()='manifest']")
        $license = $module.SelectSingleNode("./*[local-name()='license']")
        if ($null -eq $manifest) {
            throw "NBM '$($nbm.FullName)' Info/info.xml has no manifest element."
        }
        if ($null -eq $license) {
            throw "NBM '$($nbm.FullName)' Info/info.xml has no license element."
        }

        $codeName = $manifest.GetAttribute('OpenIDE-Module') -replace '/\d+$', ''
        if ($codeName -cne $module.GetAttribute('codenamebase')) {
            throw "NBM module code name '$codeName' differs from codenamebase '$($module.GetAttribute('codenamebase'))'."
        }
        $specificationVersion = $manifest.GetAttribute('OpenIDE-Module-Specification-Version')
        $implementationVersion = $manifest.GetAttribute('OpenIDE-Module-Implementation-Version')
        if ([string]::IsNullOrWhiteSpace($specificationVersion)) {
            throw "NBM '$($nbm.FullName)' has no specification version."
        }
        if ([string]::IsNullOrWhiteSpace($implementationVersion)) {
            throw "NBM '$($nbm.FullName)' has no implementation version."
        }

        $payloadFiles = [System.Collections.Generic.Dictionary[string, object]]::new(
            [System.StringComparer]::OrdinalIgnoreCase)
        foreach ($entry in $archive.Entries) {
            if (-not $entry.FullName.StartsWith(
                    'netbeans/', [System.StringComparison]::Ordinal) -or
                    $entry.FullName.EndsWith('/', [System.StringComparison]::Ordinal)) {
                continue
            }
            $relativeName = $entry.FullName.Substring('netbeans/'.Length)
            $segments = @($relativeName.Split('/'))
            if ([string]::IsNullOrWhiteSpace($relativeName) -or
                    $relativeName.Contains('\') -or
                    $segments -contains '' -or
                    $segments -contains '.' -or
                    $segments -contains '..') {
                throw "NBM '$($nbm.FullName)' contains unsafe payload path '$($entry.FullName)'."
            }
            if ($payloadFiles.ContainsKey($relativeName)) {
                throw "NBM '$($nbm.FullName)' contains duplicate payload path '$relativeName'."
            }
            $payloadStream = $entry.Open()
            try {
                $payloadFiles.Add($relativeName, [pscustomobject]@{
                        Name = $relativeName
                        Length = $entry.Length
                        Sha256 = Get-StreamSha256 $payloadStream
                    })
            } finally {
                $payloadStream.Dispose()
            }
        }
        if ($payloadFiles.Count -eq 0) {
            throw "NBM '$($nbm.FullName)' has no netbeans/ payload files."
        }

        return [pscustomobject]@{
            Path = $nbm.FullName
            FileName = $nbm.Name
            Length = $nbm.Length
            CodeName = $codeName
            SpecificationVersion = $specificationVersion
            ImplementationVersion = $implementationVersion
            ModuleName = $manifest.GetAttribute('OpenIDE-Module-Name')
            InfoDocument = $info
            PayloadFiles = $payloadFiles
        }
    } finally {
        $archive.Dispose()
    }
}

function Compare-NumericSpecificationVersion {
    param(
        [string]$Left,
        [string]$Right
    )

    if ($Left -notmatch '^\d+(?:\.\d+)*$' -or $Right -notmatch '^\d+(?:\.\d+)*$') {
        throw "Specification versions must contain dot-separated integers: '$Left', '$Right'."
    }
    $leftParts = @($Left.Split('.') | ForEach-Object { [System.Numerics.BigInteger]::Parse($_) })
    $rightParts = @($Right.Split('.') | ForEach-Object { [System.Numerics.BigInteger]::Parse($_) })
    $count = [Math]::Max($leftParts.Count, $rightParts.Count)
    for ($index = 0; $index -lt $count; $index++) {
        $leftPart = if ($index -lt $leftParts.Count) { $leftParts[$index] } else { 0 }
        $rightPart = if ($index -lt $rightParts.Count) { $rightParts[$index] } else { 0 }
        if ($leftPart -lt $rightPart) {
            return -1
        }
        if ($leftPart -gt $rightPart) {
            return 1
        }
    }
    return 0
}

function New-LocalUpdateCatalog {
    param(
        [pscustomobject]$Metadata,
        [string]$CatalogDirectory
    )

    [void](New-Item -ItemType Directory -Path $CatalogDirectory -Force)
    $catalogDirectoryItem = Get-Item -LiteralPath $CatalogDirectory
    $catalogNbmPath = Join-Path $catalogDirectoryItem.FullName $Metadata.FileName
    Copy-Item -LiteralPath $Metadata.Path -Destination $catalogNbmPath

    $catalog = [System.Xml.XmlDocument]::new()
    $catalog.XmlResolver = $null
    [void]$catalog.AppendChild($catalog.CreateXmlDeclaration('1.0', 'UTF-8', $null))
    [void]$catalog.AppendChild($catalog.CreateDocumentType(
            'module_updates',
            '-//NetBeans//DTD Autoupdate Catalog 2.8//EN',
            'https://netbeans.apache.org/dtds/autoupdate-catalog-2_8.dtd',
            $null))
    $root = $catalog.CreateElement('module_updates')
    $root.SetAttribute('timestamp', (Get-Date).ToString('ss/mm/HH/dd/MM/yyyy'))
    [void]$catalog.AppendChild($root)

    $module = $catalog.ImportNode($Metadata.InfoDocument.DocumentElement, $true)
    $module.SetAttribute('distribution', $Metadata.FileName)
    $module.SetAttribute('downloadsize', [string]$Metadata.Length)
    $licenses = @($module.SelectNodes("./*[local-name()='license']"))
    foreach ($license in $licenses) {
        [void]$module.RemoveChild($license)
    }
    foreach ($digest in @($module.SelectNodes("./*[local-name()='message_digest']"))) {
        [void]$module.RemoveChild($digest)
    }
    $messageDigest = $catalog.CreateElement('message_digest')
    $messageDigest.SetAttribute('algorithm', 'SHA-512')
    $sha512 = (Get-FileHash -LiteralPath $catalogNbmPath -Algorithm SHA512).Hash.ToLowerInvariant()
    $messageDigest.SetAttribute('value', $sha512)
    [void]$module.AppendChild($messageDigest)
    [void]$root.AppendChild($module)
    foreach ($license in $licenses) {
        [void]$root.AppendChild($catalog.ImportNode($license, $true))
    }

    $catalogPath = Join-Path $catalogDirectoryItem.FullName 'updates.xml'
    $settings = [System.Xml.XmlWriterSettings]::new()
    $settings.Encoding = [System.Text.UTF8Encoding]::new($false)
    $settings.Indent = $true
    $settings.NewLineChars = "`r`n"
    $settings.NewLineHandling = [System.Xml.NewLineHandling]::Replace
    $writer = [System.Xml.XmlWriter]::Create($catalogPath, $settings)
    try {
        $catalog.Save($writer)
    } finally {
        $writer.Dispose()
    }

    return [pscustomobject]@{
        Path = $catalogPath
        Uri = [System.Uri]::new($catalogPath).AbsoluteUri
        NbmPath = $catalogNbmPath
        Sha512 = $sha512
    }
}

function Assert-NoReparsePointAncestors {
    param(
        [string]$BoundaryPath,
        [string]$CandidatePath
    )

    $boundary = Get-CanonicalPath $BoundaryPath
    $candidate = Get-CanonicalPath $CandidatePath
    $boundaryPrefix = $boundary + [System.IO.Path]::DirectorySeparatorChar
    if (-not $candidate.StartsWith(
            $boundaryPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is outside the safety boundary '$boundary': $candidate"
    }

    $relative = [System.IO.Path]::GetRelativePath($boundary, $candidate)
    $current = $boundary
    foreach ($segment in @($relative -split '[\\/]' | Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            })) {
        $current = Join-Path $current $segment
        if (-not (Test-Path -LiteralPath $current)) {
            break
        }
        $item = Get-Item -LiteralPath $current -Force
        if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Probe path traverses a reparse point and is not isolated: $($item.FullName)"
        }
    }
}

function Resolve-IsolatedProbeRoot {
    param(
        [string]$Root,
        [string]$RequestedPath
    )

    $rootPath = Get-CanonicalPath (Resolve-Path -LiteralPath $Root).Path
    $targetPath = Get-CanonicalPath (Join-Path $rootPath 'target')
    if ([string]::IsNullOrWhiteSpace($RequestedPath)) {
        $runId = '{0}-{1}' -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $PID
        $candidate = Join-Path $targetPath "netbeans30-nbm-smoke\$runId"
    } elseif ([System.IO.Path]::IsPathRooted($RequestedPath)) {
        $candidate = Get-CanonicalPath $RequestedPath
    } else {
        $candidate = Get-CanonicalPath (Join-Path $rootPath $RequestedPath)
    }

    $targetPrefix = $targetPath.TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $candidate.StartsWith($targetPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "ProbeRoot must be a strict descendant of the repository target directory: $targetPath"
    }
    if (Test-Path -LiteralPath $candidate) {
        throw "ProbeRoot already exists; choose a new target-only path: $candidate"
    }
    Assert-NoReparsePointAncestors $rootPath $candidate
    return $candidate
}

function New-FlutterLifecycleFixture {
    param([string]$ScenarioRoot)

    $scenario = Get-CanonicalPath $ScenarioRoot
    $fixtureRoot = Join-Path $scenario 'lifecycle-fixture'
    if (Test-Path -LiteralPath $fixtureRoot) {
        throw "Flutter lifecycle fixture already exists: $fixtureRoot"
    }
    Assert-NoReparsePointAncestors $scenario $fixtureRoot

    $projectDirectory = Join-Path $fixtureRoot 'flutter_reopen_probe'
    $flutterHome = Join-Path $fixtureRoot 'sdk\flutter'
    $dartHome = Join-Path $fixtureRoot 'sdk\dart'
    foreach ($directory in @(
            (Join-Path $projectDirectory 'lib'),
            (Join-Path $flutterHome 'bin'),
            (Join-Path $dartHome 'bin'))) {
        [void][System.IO.Directory]::CreateDirectory($directory)
    }

    $utf8 = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText(
        (Join-Path $projectDirectory 'pubspec.yaml'),
        (@(
                'name: flutter_reopen_probe',
                'description: Isolated NetBeans lifecycle smoke fixture.',
                'version: 1.0.0+1',
                'environment:',
                "  sdk: '>=3.0.0 <4.0.0'",
                'dependencies:',
                '  flutter:',
                '    sdk: flutter',
                '') -join "`r`n"),
        $utf8)
    [System.IO.File]::WriteAllText(
        (Join-Path $projectDirectory 'lib\main.dart'),
        "void main() {}`r`n",
        $utf8)
    [System.IO.File]::WriteAllText(
        (Join-Path $flutterHome 'bin\flutter.bat'),
        (@(
                '@echo off',
                'if /I "%~1"=="devices" echo []',
                'if /I not "%~1"=="devices" echo []',
                'exit /b 0',
                '') -join "`r`n"),
        $utf8)
    [System.IO.File]::WriteAllText(
        (Join-Path $dartHome 'bin\dart.bat'),
        (@(
                '@echo off',
                'exit /b 0',
                '') -join "`r`n"),
        $utf8)

    foreach ($path in @($projectDirectory, $flutterHome, $dartHome)) {
        Assert-NoReparsePointAncestors $scenario $path
    }
    return [pscustomobject]@{
        Root = Get-CanonicalPath $fixtureRoot
        ProjectDirectory = Get-CanonicalPath $projectDirectory
        FlutterHome = Get-CanonicalPath $flutterHome
        DartHome = Get-CanonicalPath $dartHome
    }
}

function Assert-FlutterSdkPreferences {
    param(
        [string]$Userdir,
        [pscustomobject]$Fixture
    )

    $path = Join-Path $Userdir $FlutterPreferencesRelativePath
    $properties = Read-JavaPropertiesFile $path
    $expected = @{
        'flutter.sdk.home' = Get-CanonicalPath $Fixture.FlutterHome
        'dart.sdk.useBundled' = 'false'
        'dart.sdk.home' = Get-CanonicalPath $Fixture.DartHome
        'sdk.discovery.version' = '1'
    }
    foreach ($key in $expected.Keys) {
        if (-not $properties.ContainsKey($key)) {
            throw "Flutter SDK preferences are missing '$key': $path"
        }
        if ($properties[$key] -cne $expected[$key]) {
            throw "Flutter SDK preference '$key' is '$($properties[$key])'; expected '$($expected[$key])'."
        }
    }
}

function Assert-FlutterProjectReopenRecord {
    param(
        [string]$Userdir,
        [pscustomobject]$Fixture
    )

    $path = Join-Path $Userdir $ProjectUiPreferencesRelativePath
    $properties = Read-JavaPropertiesFile $path
    $urlKeys = @($properties.Keys | Where-Object {
            $_ -match '^openProjectsURLs\.\d+$'
        } | Sort-Object)
    if ($urlKeys.Count -ne 1) {
        throw "Expected exactly one persisted open project; found $($urlKeys.Count): $path"
    }
    try {
        $uri = [System.Uri]::new([string]$properties[$urlKeys[0]])
    } catch {
        throw "Persisted open-project URL is invalid: $($properties[$urlKeys[0]])"
    }
    if (-not $uri.IsFile) {
        throw "Persisted open-project URL is not a file URL: $uri"
    }
    $actualProject = Get-CanonicalPath $uri.LocalPath
    if (-not (Test-SameCanonicalPath $actualProject $Fixture.ProjectDirectory)) {
        throw "Persisted open project is '$actualProject'; expected '$($Fixture.ProjectDirectory)'."
    }
    $displayKeys = @($properties.Keys | Where-Object {
            $_ -match '^openProjectsDisplayNames\.\d+$'
        })
    if ($displayKeys.Count -ne 1 -or
            $properties[$displayKeys[0]] -cne 'flutter_reopen_probe') {
        throw 'Persisted Flutter project display name is missing or incorrect.'
    }
}

function Wait-FlutterLifecyclePersistence {
    param(
        [string]$Userdir,
        [pscustomobject]$Fixture,
        [bool]$RequireProject,
        [int]$TimeoutSeconds
    )

    $deadline = [System.DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastFailure = ''
    while ([System.DateTime]::UtcNow -lt $deadline) {
        try {
            Assert-FlutterSdkPreferences $Userdir $Fixture
            if ($RequireProject) {
                Assert-FlutterProjectReopenRecord $Userdir $Fixture
            }
            return
        } catch {
            $lastFailure = $_.Exception.Message
            Start-Sleep -Milliseconds 250
        }
    }
    throw "Flutter lifecycle persistence did not stabilize within $TimeoutSeconds seconds. Last failure: $lastFailure"
}

function New-ProbeRootOwnership {
    param([string]$RunRoot)

    $ownerPath = Join-Path $RunRoot '.netbeans30-smoke-owner'
    try {
        $stream = [System.IO.File]::Open(
            $ownerPath,
            [System.IO.FileMode]::CreateNew,
            [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None)
    } catch [System.IO.IOException] {
        throw "ProbeRoot is already owned by another smoke run: $RunRoot"
    }
    try {
        $ownerText = "PID=$PID`r`nStartedUtc=$([System.DateTime]::UtcNow.ToString('o'))`r`n"
        $ownerBytes = [System.Text.UTF8Encoding]::new($false).GetBytes($ownerText)
        $stream.Write($ownerBytes, 0, $ownerBytes.Length)
        $stream.Flush($true)
        return [pscustomobject]@{
            Path = $ownerPath
            Stream = $stream
        }
    } catch {
        $stream.Dispose()
        throw
    }
}

function Invoke-NetBeansCli {
    param(
        [string]$Executable,
        [string[]]$Arguments,
        [int]$TimeoutSeconds
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $Executable
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    [void]$startInfo.ArgumentList.Add('-J-Djava.awt.headless=true')
    foreach ($argument in $Arguments) {
        [void]$startInfo.ArgumentList.Add($argument)
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    try {
        [void]$process.Start()
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            # Kill only this retained CLI launcher. Kill(entireProcessTree) can
            # block indefinitely in the Windows NetBeans launcher handoff.
            $process.Kill($false)
            if (-not $process.WaitForExit(10000)) {
                throw "Timed out while stopping the retained NetBeans CLI launcher PID $($process.Id)."
            }
            $message = "NetBeans CLI command timed out after $TimeoutSeconds seconds: $($Arguments -join ' ')"
            throw [System.TimeoutException]::new($message)
        }
        [System.Threading.Tasks.Task]::WaitAll(@($stdoutTask, $stderrTask))
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            StandardOutput = $stdoutTask.Result
            StandardError = $stderrTask.Result
        }
    } finally {
        $process.Dispose()
    }
}

function Get-UnexpectedCliStderrLines {
    param([string]$StandardError)

    $unexpected = [System.Collections.Generic.List[string]]::new()
    foreach ($line in ($StandardError -split "`r?`n")) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        $harmless = $false
        foreach ($pattern in $HarmlessCliStderrPatterns) {
            if ($line -match $pattern) {
                $harmless = $true
                break
            }
        }
        if (-not $harmless) {
            $unexpected.Add($line.Trim())
        }
    }
    return $unexpected.ToArray()
}

function Assert-NetBeansCliStderr {
    param(
        [pscustomobject]$Result,
        [string]$Context
    )

    $unexpected = @(Get-UnexpectedCliStderrLines $Result.StandardError)
    if ($unexpected.Count -gt 0) {
        throw ("NetBeans CLI wrote unexpected stderr during ${Context}: " +
            ((@($unexpected | Select-Object -First 10)) -join ' | '))
    }
}

function Save-CommandEvidence {
    param(
        [string]$EvidenceDirectory,
        [string]$Name,
        [pscustomobject]$Result
    )

    [void](New-Item -ItemType Directory -Path $EvidenceDirectory -Force)
    $text = @(
        "Launcher exit code (informational): $($Result.ExitCode)",
        '--- stdout ---',
        $Result.StandardOutput,
        '--- stderr ---',
        $Result.StandardError
    ) -join "`r`n"
    [System.IO.File]::WriteAllText(
        (Join-Path $EvidenceDirectory "$Name.txt"),
        $text,
        [System.Text.UTF8Encoding]::new($false))
}

function Get-ModuleListRecord {
    param(
        [string]$Output,
        [string]$CodeName
    )

    $pattern = '^' + [System.Text.RegularExpressions.Regex]::Escape($CodeName) +
        '\s+(\S+)\s+(.+?)\s*$'
    foreach ($line in ($Output -split "`r?`n")) {
        if ($line -match $pattern) {
            return [pscustomobject]@{
                CodeName = $CodeName
                Version = $Matches[1]
                State = $Matches[2].Trim()
                Line = $line
            }
        }
    }
    return $null
}

function Test-ModuleListReady {
    param([string]$Output)
    return $Output -match '(?m)^Code Name\s+Version\s+State\s*$' -and
        $Output -match '(?m)^-{10,}\s+-{10,}\s+-{5,}\s*$'
}

function Disable-DefaultUpdateCenters {
    param([string]$Userdir)

    $providerDirectory = Join-Path $Userdir 'config\Services\AutoupdateType'
    [void](New-Item -ItemType Directory -Path $providerDirectory -Force)
    foreach ($provider in @(
            'distribution-update-provider.instance_hidden',
            'pluginportal-update-provider.instance_hidden',
            '3rdparty.instance_hidden')) {
        [System.IO.File]::WriteAllBytes(
            (Join-Path $providerDirectory $provider),
            [byte[]]::new(0))
    }
}

function ConvertFrom-WindowsCommandLine {
    param([string]$CommandLine)

    if ($null -eq ('NetBeans30Smoke.NativeCommandLine' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.ComponentModel;
using System.Runtime.InteropServices;

namespace NetBeans30Smoke {
    public static class NativeCommandLine {
        [DllImport("shell32.dll", SetLastError = true)]
        private static extern IntPtr CommandLineToArgvW(
            [MarshalAs(UnmanagedType.LPWStr)] string commandLine,
            out int argumentCount);

        [DllImport("kernel32.dll")]
        private static extern IntPtr LocalFree(IntPtr memory);

        public static string[] Split(string commandLine) {
            int count;
            IntPtr argv = CommandLineToArgvW(commandLine, out count);
            if (argv == IntPtr.Zero) {
                throw new Win32Exception(Marshal.GetLastWin32Error());
            }
            try {
                string[] result = new string[count];
                for (int index = 0; index < count; index++) {
                    IntPtr value = Marshal.ReadIntPtr(argv, index * IntPtr.Size);
                    result[index] = Marshal.PtrToStringUni(value);
                }
                return result;
            } finally {
                LocalFree(argv);
            }
        }
    }
}
'@
    }
    return [NetBeans30Smoke.NativeCommandLine]::Split($CommandLine)
}

function Get-CommandLineUserdir {
    param([string]$CommandLine)

    if ([string]::IsNullOrWhiteSpace($CommandLine)) {
        return $null
    }
    $arguments = @(ConvertFrom-WindowsCommandLine $CommandLine)
    $values = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $arguments.Count; $index++) {
        $argument = $arguments[$index]
        if ($argument -ceq '--userdir') {
            if ($index + 1 -ge $arguments.Count) {
                throw "Process command line has --userdir without a value: $CommandLine"
            }
            $values.Add((Get-CanonicalPath $arguments[++$index]))
        } elseif ($argument.StartsWith(
                '-Dnetbeans.user=', [System.StringComparison]::Ordinal)) {
            $values.Add((Get-CanonicalPath $argument.Substring('-Dnetbeans.user='.Length)))
        }
    }
    if ($values.Count -eq 0) {
        return $null
    }
    $first = $values[0]
    foreach ($value in $values) {
        if (-not (Test-SameCanonicalPath $first $value)) {
            throw "Process command line contains conflicting NetBeans userdirs: $CommandLine"
        }
    }
    return $first
}

function ConvertTo-ProcessStartTimeUtc {
    param($Value)

    if ($Value -is [System.DateTimeOffset]) {
        return $Value.UtcDateTime
    }
    if ($Value -is [System.DateTime]) {
        return $Value.ToUniversalTime()
    }
    return [System.Management.ManagementDateTimeConverter]::ToDateTime(
        [string]$Value).ToUniversalTime()
}

function Select-OwnedNetBeansProbeProcesses {
    param(
        [pscustomobject]$HostHandle,
        [object[]]$Candidates
    )

    $matching = [System.Collections.Generic.List[object]]::new()
    foreach ($candidate in @($Candidates)) {
        $candidateUserdir = Get-CommandLineUserdir $candidate.CommandLine
        if ($null -eq $candidateUserdir -or
                -not (Test-SameCanonicalPath $candidateUserdir $HostHandle.Userdir)) {
            continue
        }
        if ($candidate.Name -notmatch '^(?:netbeans(?:64)?|javaw?)\.exe$') {
            throw ("Unexpected process uses the isolated NetBeans userdir; " +
                "refusing automatic cleanup: PID $($candidate.ProcessId) $($candidate.Name)")
        }
        if ([string]::IsNullOrWhiteSpace($candidate.ExecutablePath) -or
                $null -eq $candidate.CreationDate) {
            throw "Cannot validate identity for probe process PID $($candidate.ProcessId)."
        }
        if ([System.IO.Path]::GetFileName($candidate.ExecutablePath) -ine $candidate.Name) {
            throw "Probe process executable identity is inconsistent for PID $($candidate.ProcessId)."
        }
        $createdUtc = ConvertTo-ProcessStartTimeUtc $candidate.CreationDate
        if ($createdUtc -lt $HostHandle.StartTimeUtc.AddSeconds(-2)) {
            throw "Probe process PID $($candidate.ProcessId) predates the probe host."
        }
        $matching.Add([pscustomobject]@{
                ProcessId = [int]$candidate.ProcessId
                ParentProcessId = [int]$candidate.ParentProcessId
                Name = [string]$candidate.Name
                ExecutablePath = Get-CanonicalPath $candidate.ExecutablePath
                CreationTimeUtc = $createdUtc
                CommandLine = [string]$candidate.CommandLine
            })
    }

    $ownedIds = [System.Collections.Generic.HashSet[int]]::new()
    [void]$ownedIds.Add([int]$HostHandle.Process.Id)
    $owned = [System.Collections.Generic.List[object]]::new()
    $pending = [System.Collections.Generic.List[object]]::new()
    foreach ($candidate in $matching) {
        if ($candidate.ProcessId -ne $HostHandle.Process.Id) {
            $pending.Add($candidate)
        }
    }
    do {
        $added = $false
        foreach ($candidate in @($pending.ToArray())) {
            if ($ownedIds.Contains($candidate.ParentProcessId)) {
                [void]$ownedIds.Add($candidate.ProcessId)
                $owned.Add($candidate)
                [void]$pending.Remove($candidate)
                $added = $true
            }
        }
    } while ($added)
    if ($pending.Count -gt 0) {
        throw ("Processes use the isolated userdir but are not descendants of probe PID " +
            "$($HostHandle.Process.Id): " +
            (($pending | ForEach-Object { "PID $($_.ProcessId)" }) -join ', '))
    }
    return $owned.ToArray()
}

function Test-ProbeProcessIdentity {
    param(
        [pscustomobject]$Expected,
        [pscustomobject]$Actual,
        [pscustomobject]$HostHandle
    )

    if ([int]$Actual.ProcessId -ne $Expected.ProcessId -or
            [int]$Actual.ParentProcessId -ne $Expected.ParentProcessId -or
            [string]$Actual.Name -ine $Expected.Name -or
            -not (Test-SameCanonicalPath $Actual.ExecutablePath $Expected.ExecutablePath) -or
            (ConvertTo-ProcessStartTimeUtc $Actual.CreationDate) -ne $Expected.CreationTimeUtc) {
        return $false
    }
    $actualUserdir = Get-CommandLineUserdir $Actual.CommandLine
    return $null -ne $actualUserdir -and
        (Test-SameCanonicalPath $actualUserdir $HostHandle.Userdir)
}

function Assert-HostProcessIdVacant {
    param(
        [pscustomobject]$HostHandle,
        [scriptblock]$LookupProcess
    )

    if ($null -eq $LookupProcess) {
        $LookupProcess = {
            param([int]$ProcessId)
            return @(Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)
        }
    }
    $occupants = @(& $LookupProcess ([int]$HostHandle.Process.Id))
    if ($occupants.Count -gt 0) {
        throw ("Recorded probe host PID $($HostHandle.Process.Id) is occupied after " +
            'the host exited; refusing a child sweep because the PID may have been reused.')
    }
}

function Stop-RetainedProbeProcess {
    param(
        [pscustomobject]$Expected,
        [pscustomobject]$Actual,
        [pscustomobject]$HostHandle,
        [scriptblock]$OpenProcess
    )

    if (-not (Test-ProbeProcessIdentity $Expected $Actual $HostHandle)) {
        throw "Refusing to stop PID $($Expected.ProcessId): process identity changed."
    }
    if ($null -eq $OpenProcess) {
        $OpenProcess = {
            param([int]$ProcessId)
            return [System.Diagnostics.Process]::GetProcessById($ProcessId)
        }
    }
    try {
        $retained = & $OpenProcess ([int]$Expected.ProcessId)
    } catch [System.ArgumentException] {
        return $false
    }
    if ($null -eq $retained) {
        return $false
    }
    try {
        $retained.Refresh()
        if ($retained.HasExited) {
            return $false
        }

        # Accessing SafeHandle pins this exact kernel process object. Identity is
        # validated only after the handle is retained, and Kill uses that same
        # Process instance instead of resolving a naked, reusable PID.
        $safeHandle = $retained.SafeHandle
        if ($null -eq $safeHandle -or $safeHandle.IsInvalid -or $safeHandle.IsClosed) {
            throw "Could not retain a valid handle for probe process PID $($Expected.ProcessId)."
        }
        $retained.Refresh()
        if ($retained.HasExited) {
            return $false
        }
        $retainedPath = Get-CanonicalPath $retained.MainModule.FileName
        $retainedStart = $retained.StartTime.ToUniversalTime()
        $startDeltaTicks = [Math]::Abs(
            [long]($retainedStart - $Expected.CreationTimeUtc).Ticks)
        if (-not (Test-SameCanonicalPath $retainedPath $Expected.ExecutablePath) -or
                $startDeltaTicks -gt [System.TimeSpan]::FromMilliseconds(1).Ticks) {
            throw "Refusing to stop PID $($Expected.ProcessId): retained process identity changed."
        }

        try {
            $retained.Kill($false)
        } catch [System.InvalidOperationException] {
            $retained.Refresh()
            if (-not $retained.HasExited) {
                throw
            }
            return $false
        }
        if (-not $retained.WaitForExit(10000)) {
            throw "Timed out while stopping isolated probe process PID $($Expected.ProcessId)."
        }
        return $true
    } finally {
        $retained.Dispose()
    }
}

function Start-NetBeansProbeHost {
    param(
        [string]$Executable,
        [string]$Userdir,
        [string]$Cachedir,
        [pscustomobject]$LifecycleFixture = $null
    )

    $canonicalUserdir = Get-CanonicalPath $Userdir
    $canonicalCachedir = Get-CanonicalPath $Cachedir
    foreach ($path in @($canonicalUserdir, $canonicalCachedir)) {
        [void][System.IO.Directory]::CreateDirectory($path)
    }
    $existingLock = Join-Path $canonicalUserdir 'lock'
    if (Test-Path -LiteralPath $existingLock) {
        throw "Refusing to start NetBeans with an existing isolated userdir lock: $existingLock"
    }
    Disable-DefaultUpdateCenters $canonicalUserdir
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = (Get-Item -LiteralPath $Executable).FullName
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    foreach ($argument in @(
            '--userdir', $canonicalUserdir,
            '--cachedir', $canonicalCachedir,
            '--nosplash', '--nogui', '-J-Djava.awt.headless=true')) {
        [void]$startInfo.ArgumentList.Add($argument)
    }
    if ($null -ne $LifecycleFixture) {
        [void]$startInfo.ArgumentList.Add(
            "-J-Dflutter.sdk=$($LifecycleFixture.FlutterHome)")
        [void]$startInfo.ArgumentList.Add(
            "-J-Ddart.sdk=$($LifecycleFixture.DartHome)")
    }
    $process = [System.Diagnostics.Process]::Start($startInfo)
    if ($null -eq $process) {
        throw "Could not start the NetBeans probe host: $Executable"
    }
    return [pscustomobject]@{
        Process = $process
        Executable = Get-CanonicalPath $startInfo.FileName
        StartTimeUtc = $process.StartTime.ToUniversalTime()
        Userdir = $canonicalUserdir
        Cachedir = $canonicalCachedir
    }
}

function Get-NetBeansProbeProcesses {
    param([pscustomobject]$HostHandle)

    return Select-OwnedNetBeansProbeProcesses $HostHandle @(
        Get-CimInstance Win32_Process)
}

function Test-NetBeansProbeAlive {
    param([pscustomobject]$HostHandle)

    $HostHandle.Process.Refresh()
    if (-not $HostHandle.Process.HasExited) {
        return $true
    }

    # The Windows launcher normally hosts the JVM in-process. Retain the
    # descendant fallback only for launcher/runtime variants, and accept it
    # solely when the old PID is vacant and the exact userdir is validated.
    Assert-HostProcessIdVacant $HostHandle
    return @(Get-NetBeansProbeProcesses $HostHandle).Count -gt 0
}

function Test-IsolatedNetBeansCliEndpointPublished {
    param([string]$Userdir)

    $lockPath = Join-Path (Get-CanonicalPath $Userdir) 'lock'
    if (-not (Test-Path -LiteralPath $lockPath -PathType Leaf)) {
        return $false
    }
    $lockItem = Get-Item -LiteralPath $lockPath -Force
    if ($lockItem.PSIsContainer -or
            ($lockItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
        throw "Unexpected isolated NetBeans userdir lock object: $lockPath"
    }

    # CLIHandler writes the four-byte server port only after the primary
    # launcher has acquired the userdir lock. Before that marker, a secondary
    # --modules launcher can win the single-instance race and become the host.
    return $lockItem.Length -ge 4
}

function Clear-IsolatedNetBeansUserdirLock {
    param([pscustomobject]$HostHandle)

    if (Test-NetBeansProbeAlive $HostHandle) {
        throw 'Refusing to clear an isolated userdir lock while its NetBeans probe is alive.'
    }
    $remaining = @(Get-NetBeansProbeProcesses $HostHandle)
    if ($remaining.Count -gt 0) {
        throw 'Refusing to clear an isolated userdir lock while validated probe processes remain.'
    }

    $lockPath = Join-Path $HostHandle.Userdir 'lock'
    if (-not (Test-Path -LiteralPath $lockPath)) {
        return
    }
    $lockItem = Get-Item -LiteralPath $lockPath -Force
    if ($lockItem.PSIsContainer -or
            ($lockItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
        throw "Refusing to clear an unexpected isolated userdir lock object: $lockPath"
    }
    if (-not (Test-SameCanonicalPath $lockItem.DirectoryName $HostHandle.Userdir)) {
        throw "Isolated userdir lock escaped its expected directory: $lockPath"
    }

    $deadline = [System.DateTime]::UtcNow.AddSeconds(5)
    do {
        $stream = $null
        try {
            $stream = [System.IO.FileStream]::new(
                $lockPath,
                [System.IO.FileMode]::Open,
                [System.IO.FileAccess]::ReadWrite,
                [System.IO.FileShare]::None,
                1,
                [System.IO.FileOptions]::DeleteOnClose)
            $stream.Dispose()
            $stream = $null
            if (Test-Path -LiteralPath $lockPath) {
                throw "Exclusive deletion did not remove the isolated userdir lock: $lockPath"
            }
            Write-Pass "Cleared stale lock from the isolated NetBeans userdir: $lockPath"
            return
        } catch [System.IO.IOException] {
            if ([System.DateTime]::UtcNow -ge $deadline) {
                throw "Isolated NetBeans userdir lock remained in use after cleanup: $lockPath"
            }
            Start-Sleep -Milliseconds 250
        } finally {
            if ($null -ne $stream) {
                $stream.Dispose()
            }
        }
    } while ([System.DateTime]::UtcNow -lt $deadline)
}

function Stop-NetBeansProbeHost {
    param([pscustomobject]$HostHandle)

    if ($null -eq $HostHandle -or $null -eq $HostHandle.Process) {
        return
    }
    $process = $HostHandle.Process
    try {
        $process.Refresh()
        if (-not $process.HasExited) {
            $liveProcess = Get-Process -Id $process.Id -ErrorAction SilentlyContinue
            if ($null -ne $liveProcess) {
                $livePath = $liveProcess.Path
                $liveStart = $liveProcess.StartTime.ToUniversalTime()
                if (-not (Test-SameCanonicalPath $livePath $HostHandle.Executable) -or
                        $liveStart -ne $HostHandle.StartTimeUtc) {
                    throw "Refusing to stop PID $($process.Id): it no longer matches the probe host."
                }

                [void]$process.CloseMainWindow()
                if (-not $process.WaitForExit(5000)) {
                    $process.Kill($false)
                    if (-not $process.WaitForExit(10000)) {
                        throw "Timed out while stopping isolated probe host PID $($process.Id)."
                    }
                }
            }
        }
    } finally {
        # The launcher can exit at any point before its javaw child. Always run
        # the independently validated child sweep, including on launcher races.
        $process.Refresh()
        if (-not $process.HasExited) {
            throw "Isolated NetBeans probe host PID $($process.Id) did not exit."
        }
        Assert-HostProcessIdVacant $HostHandle
        foreach ($managedProcess in @(Get-NetBeansProbeProcesses $HostHandle)) {
            $live = @(Get-CimInstance Win32_Process -Filter (
                    "ProcessId = $($managedProcess.ProcessId)"))
            if ($live.Count -eq 0) {
                continue
            }
            if ($live.Count -ne 1) {
                throw "Refusing to stop PID $($managedProcess.ProcessId): process identity changed."
            }
            Assert-HostProcessIdVacant $HostHandle
            if (Stop-RetainedProbeProcess $managedProcess $live[0] $HostHandle) {
                Write-Info ("Stopped isolated NetBeans child PID {0} ({1})." -f
                    $managedProcess.ProcessId, $managedProcess.Name)
            }
        }
        Start-Sleep -Milliseconds 250
        $remaining = @(Get-NetBeansProbeProcesses $HostHandle)
        if ($remaining.Count -gt 0) {
            throw ("Isolated NetBeans process cleanup is incomplete: " +
                (($remaining | ForEach-Object { "PID $($_.ProcessId) $($_.Name)" }) -join ', '))
        }
    }
    Write-Pass "Stopped all processes for isolated NetBeans probe PID $($process.Id)."
}

function Wait-NetBeansCliReady {
    param(
        [pscustomobject]$HostHandle,
        [string]$Executable,
        [string]$Userdir,
        [int]$StartupTimeout,
        [int]$CommandTimeout
    )

    $deadline = [System.DateTime]::UtcNow.AddSeconds($StartupTimeout)
    $lastResult = $null
    $lastFailure = ''
    $missingSince = $null
    while ([System.DateTime]::UtcNow -lt $deadline) {
        if (-not (Test-NetBeansProbeAlive $HostHandle)) {
            $lastFailure = ("NetBeans launcher exited with code " +
                "$($HostHandle.Process.ExitCode) before publishing a stable " +
                'CLI endpoint or retaining an identity-validated child process.')
            if ($null -eq $missingSince) {
                $missingSince = [System.DateTime]::UtcNow
            } elseif (([System.DateTime]::UtcNow - $missingSince).TotalSeconds -ge 3) {
                throw $lastFailure
            }
            Start-Sleep -Milliseconds 250
            continue
        }
        $missingSince = $null
        if (-not (Test-IsolatedNetBeansCliEndpointPublished $Userdir)) {
            $lastFailure = 'Waiting for the primary NetBeans launcher to publish its isolated userdir CLI endpoint.'
            Start-Sleep -Milliseconds 100
            continue
        }
        $remainingSeconds = [Math]::Ceiling(($deadline - [System.DateTime]::UtcNow).TotalSeconds)
        $attemptTimeout = [Math]::Max(1, [Math]::Min(
                [Math]::Min($CommandTimeout, 15), $remainingSeconds))
        try {
            $lastResult = Invoke-NetBeansCli $Executable @(
                '--userdir', $Userdir,
                '--cachedir', $HostHandle.Cachedir,
                '--nosplash',
                '--modules', '--list'
            ) $attemptTimeout
        } catch [System.TimeoutException] {
            $lastFailure = $_.Exception.Message
            continue
        }
        Assert-NetBeansCliStderr $lastResult 'readiness probe'
        if (Test-ModuleListReady $lastResult.StandardOutput) {
            if (-not (Test-NetBeansProbeAlive $HostHandle)) {
                $lastFailure = 'NetBeans probe disappeared while reporting CLI readiness.'
                continue
            }
            return $lastResult
        }
        Start-Sleep -Milliseconds 750
    }
    $lastStderr = if ($null -eq $lastResult) { '' } else { $lastResult.StandardError.Trim() }
    throw ("NetBeans CLI service did not become ready in $StartupTimeout seconds. " +
        "Last failure: $lastFailure Last stderr: $lastStderr")
}

function Start-ReadyNetBeansProbe {
    param(
        [string]$Executable,
        [string]$Userdir,
        [string]$Cachedir,
        [int]$StartupTimeout,
        [int]$CommandTimeout,
        [pscustomobject]$LifecycleFixture = $null
    )

    $lastFailure = ''
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $handle = $null
        try {
            $handle = Start-NetBeansProbeHost $Executable $Userdir $Cachedir `
                $LifecycleFixture
            Write-Info "Started isolated NetBeans probe attempt $attempt as PID $($handle.Process.Id)."
            $ready = Wait-NetBeansCliReady $handle $Executable $Userdir `
                $StartupTimeout $CommandTimeout
            return [pscustomobject]@{
                HostHandle = $handle
                ReadyResult = $ready
                Attempt = $attempt
            }
        } catch {
            $lastFailure = $_.Exception.Message
            if ($null -ne $handle) {
                Stop-NetBeansProbeHost $handle
                Clear-IsolatedNetBeansUserdirLock $handle
            }
            if ($attempt -lt 3) {
                Start-Sleep -Seconds 2
            }
        }
    }
    throw "NetBeans probe did not become ready after 3 isolated starts. Last failure: $lastFailure"
}

function Wait-ModuleState {
    param(
        [pscustomobject]$HostHandle,
        [string]$Executable,
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [ValidateSet('Enabled', 'Installed')]
        [string]$ExpectedState,
        [int]$TimeoutSeconds,
        [int]$CommandTimeout
    )

    $deadline = [System.DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastResult = $null
    $lastFailure = ''
    while ([System.DateTime]::UtcNow -lt $deadline) {
        if (-not (Test-NetBeansProbeAlive $HostHandle)) {
            throw "NetBeans probe host exited while waiting for $($Metadata.CodeName) to reach $ExpectedState."
        }
        $remainingSeconds = [Math]::Ceiling(($deadline - [System.DateTime]::UtcNow).TotalSeconds)
        $attemptTimeout = [Math]::Max(1, [Math]::Min(
                [Math]::Min($CommandTimeout, 30), $remainingSeconds))
        try {
            $lastResult = Invoke-NetBeansCli $Executable @(
                '--userdir', $Userdir,
                '--cachedir', $HostHandle.Cachedir,
                '--nosplash',
                '--modules', '--list'
            ) $attemptTimeout
        } catch [System.TimeoutException] {
            $lastFailure = $_.Exception.Message
            continue
        }
        Assert-NetBeansCliStderr $lastResult 'module-state probe'
        $record = Get-ModuleListRecord $lastResult.StandardOutput $Metadata.CodeName
        if ($null -ne $record -and
                $record.Version -ceq $Metadata.SpecificationVersion -and
                $record.State -ceq $ExpectedState) {
            return [pscustomobject]@{
                Result = $lastResult
                Record = $record
            }
        }
        Start-Sleep -Milliseconds 750
    }
    $lastLine = if ($null -eq $lastResult) {
        '<no module list result>'
    } else {
        $record = Get-ModuleListRecord $lastResult.StandardOutput $Metadata.CodeName
        if ($null -eq $record) { '<module absent>' } else { $record.Line }
    }
    throw ("Module did not reach $ExpectedState $($Metadata.SpecificationVersion) in " +
        "$TimeoutSeconds seconds: $lastLine Last failure: $lastFailure")
}

function Wait-ModuleEnabled {
    param(
        [pscustomobject]$HostHandle,
        [string]$Executable,
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [int]$TimeoutSeconds,
        [int]$CommandTimeout
    )

    return Wait-ModuleState $HostHandle $Executable $Userdir $Metadata `
        'Enabled' $TimeoutSeconds $CommandTimeout
}

function Get-ModuleStateMap {
    param([string]$Output)

    $states = @{}
    foreach ($line in ($Output -split "`r?`n")) {
        if ($line -match '^(\S+)\s+(\S+)\s+(Enabled|Installed)\s*$') {
            $states[$Matches[1]] = "$($Matches[2])|$($Matches[3])"
        }
    }
    return $states
}

function Assert-UnrelatedModuleStatesEqual {
    param(
        [string]$BeforeOutput,
        [string]$AfterOutput,
        [string]$ExcludedCodeName
    )

    $before = Get-ModuleStateMap $BeforeOutput
    $after = Get-ModuleStateMap $AfterOutput
    [void]$before.Remove($ExcludedCodeName)
    [void]$after.Remove($ExcludedCodeName)
    $beforeKeys = @($before.Keys | Sort-Object)
    $afterKeys = @($after.Keys | Sort-Object)
    if (($beforeKeys -join "`n") -cne ($afterKeys -join "`n")) {
        throw 'Disabling or removing Flutter changed the set of unrelated NetBeans modules.'
    }
    foreach ($key in $beforeKeys) {
        if ($before[$key] -cne $after[$key]) {
            throw "Unrelated NetBeans module '$key' changed from '$($before[$key])' to '$($after[$key])'."
        }
    }
}

function Get-CriticalLogLines {
    param([string]$LogText)
    return @($LogText -split "`r?`n" | Where-Object { $_ -match $CriticalLogPattern })
}

function Get-PluginOwnedOrderingLogLines {
    param([string]$LogText)
    return @($LogText -split "`r?`n" | Where-Object {
            $_ -match $AuxiliaryConfigurationOrderingLogPattern -or
            $_ -match $PluginLayerOrderingLogPattern
        })
}

function Test-NetBeans30ProductLog {
    param([string]$LogText)

    return $LogText -match (
        '(?m)^\s*Product Version\s*=\s*Apache\s+NetBeans IDE 30' +
        '(?:\s+\(Build [^)]+\))?\s*$')
}

function Test-NetBeansActivationLog {
    param(
        [string]$LogText,
        [pscustomobject]$Metadata
    )

    if (-not (Test-NetBeans30ProductLog $LogText)) {
        return $false
    }
    $escapedCodeName = [System.Text.RegularExpressions.Regex]::Escape(
        $Metadata.CodeName)
    $escapedSpec = [System.Text.RegularExpressions.Regex]::Escape(
        $Metadata.SpecificationVersion)
    $escapedImplementation = [System.Text.RegularExpressions.Regex]::Escape(
        $Metadata.ImplementationVersion)
    return $LogText -match (
        "(?m)^\s*$escapedCodeName\s+\[$escapedSpec\s+" +
        "$escapedImplementation(?:\s|\])")
}

function Wait-NetBeansActivationLog {
    param(
        [pscustomobject]$HostHandle,
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [int]$TimeoutSeconds
    )

    $logPath = Join-Path $Userdir 'var\log\messages.log'
    $deadline = [System.DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([System.DateTime]::UtcNow -lt $deadline) {
        if (-not (Test-NetBeansProbeAlive $HostHandle)) {
            throw "NetBeans probe host exited before its activation log was complete."
        }
        if (Test-Path -LiteralPath $logPath -PathType Leaf) {
            $log = Read-SharedTextFile $logPath
            if (Test-NetBeansActivationLog $log $Metadata) {
                return
            }
        }
        Start-Sleep -Milliseconds 250
    }
    throw ("messages.log did not identify Apache NetBeans 30 and activation of " +
        "$($Metadata.CodeName) $($Metadata.SpecificationVersion) within " +
        "$TimeoutSeconds seconds: $logPath")
}

function Assert-InstalledModuleFiles {
    param(
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [string]$ExpectedCatalogPath,
        [pscustomobject]$PreviousMetadata = $null,
        [bool]$RequireActivation = $true,
        [ValidateSet('Enabled', 'Installed')]
        [string]$ExpectedState = 'Enabled'
    )

    $configPath = Join-Path $Userdir "config\Modules\$ModuleConfigName"
    $trackingPath = Join-Path $Userdir "update_tracking\$ModuleConfigName"
    $logPath = Join-Path $Userdir 'var\log\messages.log'
    foreach ($path in @($configPath, $trackingPath, $logPath)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Installed userdir is missing: $path"
        }
    }

    $config = Read-SafeXmlFile $configPath
    if ($config.DocumentElement.GetAttribute('name') -cne $Metadata.CodeName) {
        throw "Installed config code name is '$($config.DocumentElement.GetAttribute('name'))'."
    }
    $parameters = @{}
    foreach ($parameter in @($config.DocumentElement.SelectNodes("./*[local-name()='param']"))) {
        $parameters[$parameter.GetAttribute('name')] = $parameter.InnerText.Trim()
    }
    $expectedEnabled = if ($ExpectedState -ceq 'Enabled') { 'true' } else { 'false' }
    if ($parameters['enabled'] -cne $expectedEnabled) {
        throw "Installed module enabled parameter is '$($parameters['enabled'])'."
    }
    if (-not $parameters.ContainsKey('jar')) {
        throw 'Installed module config has no jar parameter.'
    }
    $jarPath = Join-Path $Userdir ($parameters['jar'] -replace '/', '\')
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Installed module JAR does not exist: $jarPath"
    }

    $tracking = Read-SafeXmlFile $trackingPath
    if ($tracking.DocumentElement.GetAttribute('codename') -cne $Metadata.CodeName) {
        throw "Update tracking code name is '$($tracking.DocumentElement.GetAttribute('codename'))'."
    }
    $lastVersion = $tracking.SelectSingleNode(
        "/*[local-name()='module']/*[local-name()='module_version'][@last='true']")
    if ($null -eq $lastVersion) {
        throw 'Update tracking has no last=true module_version.'
    }
    if ($lastVersion.GetAttribute('specification_version') -cne $Metadata.SpecificationVersion) {
        throw "Tracked specification version is '$($lastVersion.GetAttribute('specification_version'))'."
    }
    $catalogTrackingVersion = $null
    foreach ($trackingVersion in @($tracking.SelectNodes(
                "/*[local-name()='module']/*[local-name()='module_version']"))) {
        if ($trackingVersion.GetAttribute('specification_version') -cne
                $Metadata.SpecificationVersion) {
            continue
        }
        $origin = $trackingVersion.GetAttribute('origin')
        try {
            $originUri = [System.Uri]::new($origin)
            $originPath = [System.IO.Path]::GetFullPath($originUri.LocalPath)
        } catch {
            continue
        }
        if ($originUri.IsFile -and
                (Test-SameCanonicalPath $originPath $ExpectedCatalogPath)) {
            $catalogTrackingVersion = $trackingVersion
            break
        }
    }
    if ($null -eq $catalogTrackingVersion) {
        throw "Update tracking does not identify the staged catalog '$ExpectedCatalogPath'."
    }
    $trackedFiles = @($lastVersion.SelectNodes("./*[local-name()='file']"))
    if ($trackedFiles.Count -eq 0) {
        throw 'Update tracking does not list installed files.'
    }
    $trackedNames = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    foreach ($trackedFile in $trackedFiles) {
        $trackedName = $trackedFile.GetAttribute('name').Replace('\', '/')
        if ([string]::IsNullOrWhiteSpace($trackedName) -or
                $trackedName.StartsWith('/', [System.StringComparison]::Ordinal) -or
                @($trackedName.Split('/')) -contains '..') {
            throw "Update tracking contains unsafe file path '$trackedName'."
        }
        if (-not $trackedNames.Add($trackedName)) {
            throw "Update tracking contains duplicate file '$trackedName'."
        }
        if (-not $Metadata.PayloadFiles.ContainsKey($trackedName)) {
            throw "Update tracking contains file not present in the NBM payload: $trackedName"
        }
        $installedPath = Join-Path $Userdir ($trackedName -replace '/', '\')
        if (-not (Test-Path -LiteralPath $installedPath -PathType Leaf)) {
            throw "Update tracking references a missing file: $installedPath"
        }
        $expectedPayload = $Metadata.PayloadFiles[$trackedName]
        $moduleConfigPayload = "config/Modules/$ModuleConfigName"
        $moduleTrackingPayload = "update_tracking/$ModuleConfigName"
        $mutablePayload = $trackedName -ieq $moduleTrackingPayload -or
            ($ExpectedState -ceq 'Installed' -and
                $trackedName -ieq $moduleConfigPayload)
        if (-not $mutablePayload) {
            $installedItem = Get-Item -LiteralPath $installedPath
            if ($installedItem.Length -ne $expectedPayload.Length) {
                throw "Installed payload length differs from the NBM for '$trackedName'."
            }
            $installedSha256 = (Get-FileHash -LiteralPath $installedPath -Algorithm SHA256).Hash
            if ($installedSha256 -ine $expectedPayload.Sha256) {
                throw "Installed payload SHA-256 differs from the NBM for '$trackedName'."
            }
        }
    }
    if ($trackedNames.Count -ne $Metadata.PayloadFiles.Count) {
        $missingNames = @($Metadata.PayloadFiles.Keys | Where-Object {
                -not $trackedNames.Contains($_)
            })
        throw ("Update tracking omits NBM payload file(s): " + ($missingNames -join ', '))
    }
    if ($null -ne $PreviousMetadata) {
        foreach ($previousName in $PreviousMetadata.PayloadFiles.Keys) {
            if ($Metadata.PayloadFiles.ContainsKey($previousName)) {
                continue
            }
            $stalePath = Join-Path $Userdir ($previousName -replace '/', '\')
            if (Test-Path -LiteralPath $stalePath -PathType Leaf) {
                throw "Upgrade left stale previous-version payload file: $stalePath"
            }
        }
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jarArchive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $manifest = Get-ManifestAttributes (Read-ZipEntryText $jarArchive 'META-INF/MANIFEST.MF')
        $jarCodeName = $manifest['OpenIDE-Module'] -replace '/\d+$', ''
        if ($jarCodeName -cne $Metadata.CodeName) {
            throw "Installed JAR code name is '$jarCodeName'."
        }
        if ($manifest['OpenIDE-Module-Specification-Version'] -cne
                $Metadata.SpecificationVersion) {
            throw "Installed JAR specification version is '$($manifest['OpenIDE-Module-Specification-Version'])'."
        }
        if ($manifest['OpenIDE-Module-Implementation-Version'] -cne
                $Metadata.ImplementationVersion) {
            throw "Installed JAR implementation version is '$($manifest['OpenIDE-Module-Implementation-Version'])'."
        }
    } finally {
        $jarArchive.Dispose()
    }

    $log = Read-SharedTextFile $logPath
    if (-not (Test-NetBeans30ProductLog $log)) {
        throw 'messages.log does not identify Apache NetBeans 30.'
    }
    if ($RequireActivation -and
            -not (Test-NetBeansActivationLog $log $Metadata)) {
        throw "messages.log has no activation line for $($Metadata.CodeName) $($Metadata.SpecificationVersion) $($Metadata.ImplementationVersion)."
    }
    $criticalLines = @(Get-CriticalLogLines $log)
    if ($criticalLines.Count -gt 0) {
        throw "messages.log contains critical pattern(s): $((@($criticalLines | Select-Object -First 5)) -join ' | ')"
    }
    $pluginOrderingLines = @(Get-PluginOwnedOrderingLogLines $log)
    if ($pluginOrderingLines.Count -gt 0) {
        throw ("messages.log contains plugin-owned Ordering warning(s): " +
            ((@($pluginOrderingLines | Select-Object -First 5)) -join ' | '))
    }

    $scope = if ($RequireActivation) {
        "installed $ExpectedState payload hashes, tracking, JAR metadata, and activation log"
    } else {
        "installed $ExpectedState payload hashes, tracking, JAR metadata, and NetBeans 30 product log"
    }
    Write-Pass ("{0} match {1} {2}." -f $scope, $Metadata.CodeName,
        $Metadata.SpecificationVersion)
}

function Wait-InstalledModuleFiles {
    param(
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [string]$ExpectedCatalogPath,
        [pscustomobject]$PreviousMetadata,
        [int]$TimeoutSeconds,
        [ValidateSet('Enabled', 'Installed')]
        [string]$ExpectedState = 'Enabled'
    )

    $deadline = [System.DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastFailure = ''
    while ([System.DateTime]::UtcNow -lt $deadline) {
        try {
            Assert-InstalledModuleFiles $Userdir $Metadata $ExpectedCatalogPath `
                $PreviousMetadata -RequireActivation $false `
                -ExpectedState $ExpectedState
            return
        } catch {
            $lastFailure = $_.Exception.Message
            Start-Sleep -Milliseconds 250
        }
    }
    throw ("Updated payload for $($Metadata.CodeName) " +
        "$($Metadata.SpecificationVersion) did not stabilize within " +
        "$TimeoutSeconds seconds. Last failure: $lastFailure")
}

function Assert-FinalLogClean {
    param([string]$Userdir)

    $logDirectory = Join-Path $Userdir 'var\log'
    $logPaths = @(Get-ChildItem -LiteralPath $logDirectory `
            -Filter 'messages.log*' -File -ErrorAction SilentlyContinue)
    if ($logPaths.Count -eq 0) {
        throw "NetBeans messages.log is missing after shutdown: $logDirectory"
    }
    $criticalEvidence = [System.Collections.Generic.List[string]]::new()
    $pluginOrderingEvidence = [System.Collections.Generic.List[string]]::new()
    foreach ($logPath in $logPaths) {
        $logText = Read-SharedTextFile $logPath.FullName
        foreach ($line in @(Get-CriticalLogLines $logText)) {
            $criticalEvidence.Add("$($logPath.Name): $line")
        }
        foreach ($line in @(Get-PluginOwnedOrderingLogLines $logText)) {
            $pluginOrderingEvidence.Add("$($logPath.Name): $line")
        }
    }
    if ($criticalEvidence.Count -gt 0) {
        throw ("NetBeans logs contain critical pattern(s) after shutdown: " +
            ((@($criticalEvidence | Select-Object -First 5)) -join ' | '))
    }
    if ($pluginOrderingEvidence.Count -gt 0) {
        throw ("NetBeans logs contain plugin-owned Ordering warning(s) " +
            "after shutdown: " +
            ((@($pluginOrderingEvidence | Select-Object -First 5)) -join ' | '))
    }
    Write-Pass ('All NetBeans messages.log sessions remain free of configured ' +
        'critical error patterns and plugin-owned Ordering warnings after shutdown.')
}

function Invoke-InstallOrUpdate {
    param(
        [string]$Operation,
        [string]$Executable,
        [string]$Userdir,
        [string]$Cachedir,
        [pscustomobject]$Catalog,
        [pscustomobject]$Metadata,
        [int]$TimeoutSeconds
    )

    $matcher = '^' + [System.Text.RegularExpressions.Regex]::Escape($Metadata.CodeName) + '$'
    if ($Operation -ceq 'install') {
        $successPattern = '(?m)^Installing\s+' +
            [System.Text.RegularExpressions.Regex]::Escape($Metadata.CodeName) + '@' +
            [System.Text.RegularExpressions.Regex]::Escape($Metadata.SpecificationVersion) + '\s*$'
    } else {
        $successPattern = '(?m)^Will update\s+' +
            [System.Text.RegularExpressions.Regex]::Escape($Metadata.CodeName) +
            '@\S+\s+to version\s+' +
            [System.Text.RegularExpressions.Regex]::Escape($Metadata.SpecificationVersion) + '\s*$'
    }

    $deadline = [System.DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $lastResult = $null
    $lastFailure = ''
    $attemptsMade = 0
    for ($attempt = 1; $attempt -le 3 -and
            [System.DateTime]::UtcNow -lt $deadline; $attempt++) {
        $attemptsMade = $attempt
        $remainingSeconds = [Math]::Ceiling(
            ($deadline - [System.DateTime]::UtcNow).TotalSeconds)
        $attemptTimeout = [Math]::Max(1, [Math]::Min(30, $remainingSeconds))
        try {
            $lastResult = Invoke-NetBeansCli $Executable @(
                '--userdir', $Userdir,
                '--cachedir', $Cachedir,
                '--nosplash',
                '--modules',
                '--extra-uc', $Catalog.Uri,
                "--$Operation", $matcher
            ) $attemptTimeout
        } catch [System.TimeoutException] {
            $lastFailure = $_.Exception.Message
            if ($Operation -ceq 'update') {
                # An update can intentionally stop the serving NetBeans JVM,
                # leaving its Windows CLI launcher waiting. The caller must
                # restart NetBeans and verify the exact target version/files.
                return [pscustomobject]@{
                    ExitCode = $null
                    StandardOutput = 'Update CLI timed out during the NetBeans restart handoff; exact post-restart verification is required.'
                    StandardError = $lastFailure
                    TimedOut = $true
                }
            }
            continue
        }
        $unexpectedStderr = @(Get-UnexpectedCliStderrLines `
            $lastResult.StandardError)
        $catalogMiss = "Cannot find any module matching [$matcher]"
        if ($unexpectedStderr.Count -eq 1 -and
                $unexpectedStderr[0] -ceq $catalogMiss) {
            $lastFailure = $catalogMiss
            if ($attempt -lt 3 -and [System.DateTime]::UtcNow -lt $deadline) {
                Start-Sleep -Milliseconds 750
            }
            continue
        }
        Assert-NetBeansCliStderr $lastResult "$Operation operation"
        if ($lastResult.StandardOutput -match $successPattern) {
            return $lastResult
        }
        if (-not [string]::IsNullOrWhiteSpace($lastResult.StandardOutput)) {
            break
        }

        # A just-started NetBeans CLI endpoint can transiently return no output.
        # Before retrying a mutating operation, verify whether it already took effect.
        try {
            $state = Invoke-NetBeansCli $Executable @(
                '--userdir', $Userdir,
                '--cachedir', $Cachedir,
                '--nosplash',
                '--modules', '--list'
            ) $attemptTimeout
            Assert-NetBeansCliStderr $state "$Operation state verification"
            $record = Get-ModuleListRecord $state.StandardOutput $Metadata.CodeName
            if ($null -ne $record -and
                    $record.Version -ceq $Metadata.SpecificationVersion -and
                    $record.State -ceq 'Enabled') {
                return [pscustomobject]@{
                    ExitCode = $lastResult.ExitCode
                    StandardOutput = "Operation returned no output; verified module state: $($record.Line)"
                    StandardError = $lastResult.StandardError
                }
            }
        } catch [System.TimeoutException] {
            $lastFailure = $_.Exception.Message
        }
        if ($attempt -lt 3 -and [System.DateTime]::UtcNow -lt $deadline) {
            Start-Sleep -Milliseconds 750
        }
    }

    $summary = if ($null -eq $lastResult -or
            [string]::IsNullOrWhiteSpace($lastResult.StandardOutput)) {
        '<no output>'
    } else {
        (($lastResult.StandardOutput -split "`r?`n" |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
                Select-Object -Last 10) -join ' | ')
    }
    throw ("NetBeans did not confirm $Operation of $($Metadata.CodeName) " +
        "$($Metadata.SpecificationVersion) after $attemptsMade attempt(s). " +
        "Output: $summary Last failure: $lastFailure")
}

function Invoke-DirectDisableModule {
    param(
        [string]$Executable,
        [string]$Userdir,
        [string]$Cachedir,
        [pscustomobject]$Metadata,
        [int]$TimeoutSeconds
    )

    $matcher = '^' + [System.Text.RegularExpressions.Regex]::Escape(
        $Metadata.CodeName) + '$'
    $result = Invoke-NetBeansCli $Executable @(
        '--userdir', $Userdir,
        '--cachedir', $Cachedir,
        '--nosplash',
        '--modules', '--direct-disable', $matcher
    ) $TimeoutSeconds
    Assert-NetBeansCliStderr $result 'direct module disable'
    return $result
}

function Get-TrackedModulePayloadNames {
    param(
        [string]$Userdir,
        [pscustomobject]$Metadata
    )

    $trackingPath = Join-Path $Userdir "update_tracking\$ModuleConfigName"
    $tracking = Read-SafeXmlFile $trackingPath
    if ($tracking.DocumentElement.GetAttribute('codename') -cne $Metadata.CodeName) {
        throw "Update tracking code name is '$($tracking.DocumentElement.GetAttribute('codename'))'."
    }
    $lastVersion = $tracking.SelectSingleNode(
        "/*[local-name()='module']/*[local-name()='module_version'][@last='true']")
    if ($null -eq $lastVersion -or
            $lastVersion.GetAttribute('specification_version') -cne
            $Metadata.SpecificationVersion) {
        throw 'Update tracking does not identify the exact installed module version.'
    }

    $names = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    foreach ($file in @($lastVersion.SelectNodes("./*[local-name()='file']"))) {
        $name = $file.GetAttribute('name').Replace('\', '/')
        if ([string]::IsNullOrWhiteSpace($name) -or
                $name.StartsWith('/', [System.StringComparison]::Ordinal) -or
                [System.IO.Path]::IsPathRooted($name) -or
                @($name.Split('/')) -contains '..') {
            throw "Update tracking contains unsafe file path '$name'."
        }
        if (-not $names.Add($name)) {
            throw "Update tracking contains duplicate file '$name'."
        }
    }
    if ($names.Count -ne $Metadata.PayloadFiles.Count) {
        throw 'Update tracking payload count differs from the exact NBM payload.'
    }
    foreach ($expectedName in $Metadata.PayloadFiles.Keys) {
        if (-not $names.Contains($expectedName)) {
            throw "Update tracking omits exact NBM payload file '$expectedName'."
        }
    }
    return @($names | Sort-Object)
}

function Remove-EmptyParentDirectories {
    param(
        [string]$StartDirectory,
        [string]$BoundaryDirectory
    )

    $boundary = Get-CanonicalPath $BoundaryDirectory
    $current = Get-CanonicalPath $StartDirectory
    while (-not (Test-SameCanonicalPath $current $boundary)) {
        Assert-NoReparsePointAncestors $boundary $current
        if (-not (Test-Path -LiteralPath $current -PathType Container)) {
            $current = Split-Path -Parent $current
            continue
        }
        if (@([System.IO.Directory]::EnumerateFileSystemEntries($current)).Count -gt 0) {
            break
        }
        [System.IO.Directory]::Delete($current, $false)
        $current = Split-Path -Parent $current
    }
}

function Assert-IsolatedModuleRemoved {
    param(
        [string]$Userdir,
        [pscustomobject]$Metadata
    )

    foreach ($name in $Metadata.PayloadFiles.Keys) {
        foreach ($path in @(
                (Join-Path $Userdir ($name -replace '/', '\')),
                (Join-Path $Userdir (
                        'update\backup\netbeans\' + ($name -replace '/', '\'))))) {
            if (Test-Path -LiteralPath $path) {
                throw "Removed Flutter module left a payload file: $path"
            }
        }
    }
    $trackingPath = Join-Path $Userdir "update_tracking\$ModuleConfigName"
    if (Test-Path -LiteralPath $trackingPath) {
        throw "Removed Flutter module left update tracking: $trackingPath"
    }
}

function Remove-IsolatedInstalledModule {
    param(
        [string]$ScenarioRoot,
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [string]$CatalogPath,
        [pscustomobject]$StoppedHostHandle
    )

    $scenario = Get-CanonicalPath $ScenarioRoot
    $canonicalUserdir = Get-CanonicalPath $Userdir
    Assert-NoReparsePointAncestors $scenario $canonicalUserdir
    if ($null -eq $StoppedHostHandle -or
            $null -eq $StoppedHostHandle.Process -or
            [string]::IsNullOrWhiteSpace($StoppedHostHandle.Userdir)) {
        throw 'Offline module cleanup requires the identity-validated handle of the stopped isolated NetBeans host.'
    }
    if (-not (Test-SameCanonicalPath $StoppedHostHandle.Userdir $canonicalUserdir)) {
        throw 'Stopped NetBeans host handle does not belong to the cleanup userdir.'
    }
    $probeAlive = Test-NetBeansProbeAlive $StoppedHostHandle
    $remainingProbeProcesses = @(
        Get-NetBeansProbeProcesses $StoppedHostHandle)
    if ($probeAlive -or $remainingProbeProcesses.Count -gt 0) {
        throw 'Refusing offline module cleanup while the isolated NetBeans probe is alive.'
    }

    foreach ($markerName in @('to_uninstall.txt', 'to_disable.txt', 'to_enable.txt')) {
        $marker = Join-Path $canonicalUserdir "update\deactivate\$markerName"
        if (Test-Path -LiteralPath $marker) {
            throw "Refusing cleanup with a pending NetBeans updater marker: $marker"
        }
    }

    Assert-InstalledModuleFiles $canonicalUserdir $Metadata $CatalogPath $null `
        -RequireActivation $false -ExpectedState 'Installed'
    $trackedNames = @(Get-TrackedModulePayloadNames $canonicalUserdir $Metadata)

    $trackingDirectory = Join-Path $canonicalUserdir 'update_tracking'
    foreach ($otherTracking in @(Get-ChildItem -LiteralPath $trackingDirectory `
            -Filter '*.xml' -File -ErrorAction SilentlyContinue)) {
        if ($otherTracking.Name -ieq $ModuleConfigName) {
            continue
        }
        $otherDocument = Read-SafeXmlFile $otherTracking.FullName
        foreach ($trackedFile in @($otherDocument.SelectNodes(
                    "//*[local-name()='file']"))) {
            $otherName = $trackedFile.GetAttribute('name').Replace('\', '/')
            if ($trackedNames -icontains $otherName) {
                throw "Another module update-tracking file owns Flutter payload '$otherName': $($otherTracking.FullName)"
            }
        }
    }

    $pathsToRemove = [System.Collections.Generic.List[string]]::new()
    $uniquePathsToRemove = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    foreach ($name in $trackedNames) {
        $currentPath = Get-CanonicalPath (
            Join-Path $canonicalUserdir ($name -replace '/', '\'))
        Assert-NoReparsePointAncestors $canonicalUserdir $currentPath
        $currentItem = Get-Item -LiteralPath $currentPath -Force
        if ($currentItem.PSIsContainer -or
                ($currentItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
            throw "Tracked Flutter payload is not a regular isolated file: $currentPath"
        }
        if ($uniquePathsToRemove.Add($currentPath)) {
            $pathsToRemove.Add($currentPath)
        }

        $backupPath = Get-CanonicalPath (Join-Path $canonicalUserdir (
                'update\backup\netbeans\' + ($name -replace '/', '\')))
        Assert-NoReparsePointAncestors $canonicalUserdir $backupPath
        if (Test-Path -LiteralPath $backupPath) {
            $backupItem = Get-Item -LiteralPath $backupPath -Force
            if ($backupItem.PSIsContainer -or
                    ($backupItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
                throw "Flutter backup payload is not a regular isolated file: $backupPath"
            }
            $trackingPayload = "update_tracking/$ModuleConfigName"
            if ($name -ieq $trackingPayload) {
                $backupTracking = Read-SafeXmlFile $backupPath
                $backupLast = $backupTracking.SelectSingleNode(
                    "/*[local-name()='module']/*[local-name()='module_version'][@last='true']")
                if ($null -eq $backupLast) {
                    throw "Flutter backup update tracking has no last=true version: $backupPath"
                }
                $backupNames = @($backupLast.SelectNodes(
                        "./*[local-name()='file']") | ForEach-Object {
                        $_.GetAttribute('name').Replace('\', '/')
                    } | Sort-Object -Unique)
                if ($backupTracking.DocumentElement.GetAttribute('codename') -cne
                        $Metadata.CodeName -or
                        $backupLast.GetAttribute('specification_version') -cne
                        $Metadata.SpecificationVersion -or
                        ($backupNames -join "`n") -cne
                        (@($trackedNames | Sort-Object) -join "`n")) {
                    throw "Flutter backup update tracking is not the exact installed module layout: $backupPath"
                }
            } else {
                $expected = $Metadata.PayloadFiles[$name]
                if ($backupItem.Length -ne $expected.Length -or
                        (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash -ine
                        $expected.Sha256) {
                    throw "Flutter backup payload differs from the exact NBM: $backupPath"
                }
            }
            if ($uniquePathsToRemove.Add($backupPath)) {
                $pathsToRemove.Add($backupPath)
            }
        }
    }

    $trackingPath = Get-CanonicalPath (
        Join-Path $canonicalUserdir "update_tracking\$ModuleConfigName")
    Assert-NoReparsePointAncestors $canonicalUserdir $trackingPath
    if ($uniquePathsToRemove.Add($trackingPath)) {
        $pathsToRemove.Add($trackingPath)
    }

    $removed = [System.Collections.Generic.List[string]]::new()
    foreach ($path in $pathsToRemove) {
        [System.IO.File]::Delete($path)
        if (Test-Path -LiteralPath $path) {
            throw "Could not remove isolated Flutter module file: $path"
        }
        $removed.Add($path)
    }
    foreach ($parent in @($removed | ForEach-Object {
                Split-Path -Parent $_
            } | Sort-Object Length -Descending -Unique)) {
        Remove-EmptyParentDirectories $parent $canonicalUserdir
    }

    Assert-IsolatedModuleRemoved $canonicalUserdir $Metadata
    return [pscustomobject]@{
        RemovedPaths = $removed.ToArray()
    }
}

function Invoke-IsolatedDisableCleanup {
    param(
        [string]$Executable,
        [string]$ScenarioRoot,
        [pscustomobject]$Metadata,
        [pscustomobject]$Catalog,
        [pscustomobject]$LifecycleFixture,
        [string]$EvidenceDirectory,
        [int]$StartupTimeout,
        [int]$CommandTimeout
    )

    $cleanupRoot = Join-Path $ScenarioRoot 'disable-uninstall-cleanup'
    $cleanupUserdir = Join-Path $cleanupRoot 'userdir'
    $cleanupCache = Join-Path $cleanupRoot 'cache'
    [void][System.IO.Directory]::CreateDirectory($cleanupRoot)
    $hostHandle = $null
    $stoppedHost = $null
    try {
        $started = Start-ReadyNetBeansProbe $Executable $cleanupUserdir `
            $cleanupCache $StartupTimeout $CommandTimeout $LifecycleFixture
        $hostHandle = $started.HostHandle
        Save-CommandEvidence $EvidenceDirectory 'cleanup-list-ready' `
            $started.ReadyResult

        $install = Invoke-InstallOrUpdate 'install' $Executable $cleanupUserdir `
            $hostHandle.Cachedir $Catalog $Metadata $CommandTimeout
        Save-CommandEvidence $EvidenceDirectory 'cleanup-install-current' $install
        $enabled = Wait-ModuleState $hostHandle $Executable $cleanupUserdir `
            $Metadata 'Enabled' $CommandTimeout $CommandTimeout
        Wait-NetBeansActivationLog $hostHandle $cleanupUserdir $Metadata `
            ([Math]::Min($CommandTimeout, 30))
        Assert-InstalledModuleFiles $cleanupUserdir $Metadata $Catalog.Path
        Wait-FlutterLifecyclePersistence $cleanupUserdir $LifecycleFixture $false `
            ([Math]::Min($CommandTimeout, 30))
        $enabled = Wait-ModuleState $hostHandle $Executable $cleanupUserdir `
            $Metadata 'Enabled' $CommandTimeout $CommandTimeout
        Save-CommandEvidence $EvidenceDirectory 'cleanup-list-enabled' $enabled.Result

        $disable = Invoke-DirectDisableModule $Executable $cleanupUserdir `
            $hostHandle.Cachedir $Metadata $CommandTimeout
        Save-CommandEvidence $EvidenceDirectory 'cleanup-disable-current' $disable
        $disabled = Wait-ModuleState $hostHandle $Executable $cleanupUserdir `
            $Metadata 'Installed' $CommandTimeout $CommandTimeout
        Save-CommandEvidence $EvidenceDirectory 'cleanup-list-disabled' $disabled.Result
        Assert-UnrelatedModuleStatesEqual $enabled.Result.StandardOutput `
            $disabled.Result.StandardOutput $Metadata.CodeName
        Wait-InstalledModuleFiles $cleanupUserdir $Metadata $Catalog.Path $null `
            ([Math]::Min($CommandTimeout, 30)) -ExpectedState 'Installed'
        Assert-FlutterSdkPreferences $cleanupUserdir $LifecycleFixture
        Write-Pass 'NetBeans reports the Flutter module Installed (disabled) without changing unrelated module versions.'

        $stoppedHost = $hostHandle
        Stop-NetBeansProbeHost $hostHandle
        Clear-IsolatedNetBeansUserdirLock $hostHandle
        $hostHandle = $null

        $cleanup = Remove-IsolatedInstalledModule $cleanupRoot $cleanupUserdir `
            $Metadata $Catalog.Path $stoppedHost
        Save-CommandEvidence $EvidenceDirectory 'offline-uninstall-cleanup' `
            ([pscustomobject]@{
                    ExitCode = 0
                    StandardOutput = ($cleanup.RemovedPaths -join "`r`n")
                    StandardError = ''
                })
        Assert-IsolatedModuleRemoved $cleanupUserdir $Metadata
        Assert-FlutterSdkPreferences $cleanupUserdir $LifecycleFixture
        Write-Pass "Removed only $($cleanup.RemovedPaths.Count) verified Flutter module/tracking files; SDK preferences remain."

        $postCleanupCache = Join-Path $cleanupRoot 'post-cleanup-cache'
        $postCleanup = Start-ReadyNetBeansProbe $Executable $cleanupUserdir `
            $postCleanupCache $StartupTimeout $CommandTimeout
        $hostHandle = $postCleanup.HostHandle
        Save-CommandEvidence $EvidenceDirectory 'cleanup-list-after-removal' `
            $postCleanup.ReadyResult
        $removedRecord = Get-ModuleListRecord `
            $postCleanup.ReadyResult.StandardOutput $Metadata.CodeName
        if ($null -ne $removedRecord) {
            throw ("Removed Flutter module is still reported by NetBeans: " +
                $removedRecord.Line)
        }
        Write-Pass 'A fresh-cache NetBeans restart no longer reports the removed Flutter module.'
        Stop-NetBeansProbeHost $hostHandle
        Clear-IsolatedNetBeansUserdirLock $hostHandle
        $hostHandle = $null
        Assert-FlutterSdkPreferences $cleanupUserdir $LifecycleFixture
    } finally {
        if ($null -ne $hostHandle) {
            Stop-NetBeansProbeHost $hostHandle
            Clear-IsolatedNetBeansUserdirLock $hostHandle
        }
    }
    Assert-FinalLogClean $cleanupUserdir
    return [pscustomobject]@{
        Root = $cleanupRoot
        Userdir = $cleanupUserdir
    }
}

function Invoke-SmokeScenario {
    param(
        [string]$Name,
        [string]$Executable,
        [string]$ScenarioRoot,
        [pscustomobject]$CurrentMetadata,
        [pscustomobject]$PreviousMetadata,
        [int]$StartupTimeout,
        [int]$CommandTimeout
    )

    $userdir = Join-Path $ScenarioRoot 'userdir'
    $cachedir = Join-Path $ScenarioRoot 'cache'
    $evidence = Join-Path $ScenarioRoot 'commands'
    [void][System.IO.Directory]::CreateDirectory($ScenarioRoot)

    $currentCatalog = New-LocalUpdateCatalog $CurrentMetadata (
        Join-Path $ScenarioRoot 'catalog-current')
    $previousCatalog = if ($null -eq $PreviousMetadata) {
        $null
    } else {
        New-LocalUpdateCatalog $PreviousMetadata (Join-Path $ScenarioRoot 'catalog-previous')
    }
    $lifecycleFixture = if ($null -eq $PreviousMetadata) {
        New-FlutterLifecycleFixture $ScenarioRoot
    } else {
        $null
    }

    Write-Info "Starting $Name scenario in $ScenarioRoot"
    $hostHandle = $null
    $cleanupScenario = $null
    try {
        $started = Start-ReadyNetBeansProbe $Executable $userdir $cachedir `
            $StartupTimeout $CommandTimeout $lifecycleFixture
        $hostHandle = $started.HostHandle
        $ready = $started.ReadyResult
        Save-CommandEvidence $evidence 'list-ready' $ready
        Write-Pass 'NetBeans 30 command-line module service is ready.'

        if ($null -ne $PreviousMetadata) {
            $installPrevious = Invoke-InstallOrUpdate 'install' $Executable $userdir `
                $hostHandle.Cachedir $previousCatalog $PreviousMetadata $CommandTimeout
            Save-CommandEvidence $evidence 'install-previous' $installPrevious
            $previousEnabled = Wait-ModuleEnabled $hostHandle $Executable $userdir `
                $PreviousMetadata $CommandTimeout $CommandTimeout
            Save-CommandEvidence $evidence 'list-after-previous' $previousEnabled.Result
            Write-Pass "Module list reports previous version $($PreviousMetadata.SpecificationVersion) Enabled."
            Wait-NetBeansActivationLog $hostHandle $userdir $PreviousMetadata `
                ([Math]::Min($CommandTimeout, 30))
            Assert-InstalledModuleFiles $userdir $PreviousMetadata $previousCatalog.Path

            $updateCurrent = Invoke-InstallOrUpdate 'update' $Executable $userdir `
                $hostHandle.Cachedir $currentCatalog $CurrentMetadata $CommandTimeout
            Save-CommandEvidence $evidence 'update-current' $updateCurrent

            # Updating an active module can stop the serving JVM. Validate the
            # persisted transition offline; clean-install activation of the
            # same current NBM is proven by the separate scenario.
            Stop-NetBeansProbeHost $hostHandle
            Clear-IsolatedNetBeansUserdirLock $hostHandle
            $hostHandle = $null
            Wait-InstalledModuleFiles $userdir $CurrentMetadata `
                $currentCatalog.Path $PreviousMetadata `
                ([Math]::Min($CommandTimeout, 30))
            Write-Pass (("Offline upgrade payload is exactly {0}; current-version " +
                'activation is covered by the clean-install scenario.') -f
                $CurrentMetadata.SpecificationVersion)
        } else {
            $installCurrent = Invoke-InstallOrUpdate 'install' $Executable $userdir `
                $hostHandle.Cachedir $currentCatalog $CurrentMetadata $CommandTimeout
            Save-CommandEvidence $evidence 'install-current' $installCurrent
            $currentEnabled = Wait-ModuleEnabled $hostHandle $Executable $userdir `
                $CurrentMetadata $CommandTimeout $CommandTimeout
            Save-CommandEvidence $evidence 'list-after-current' `
                $currentEnabled.Result
            Write-Pass "Module list reports current version $($CurrentMetadata.SpecificationVersion) Enabled."
            Wait-NetBeansActivationLog $hostHandle $userdir $CurrentMetadata `
                ([Math]::Min($CommandTimeout, 30))
            Assert-InstalledModuleFiles $userdir $CurrentMetadata `
                $currentCatalog.Path

            Wait-FlutterLifecyclePersistence $userdir $lifecycleFixture $false `
                ([Math]::Min($CommandTimeout, 30))
            Write-Pass 'Auto-discovered Flutter and Dart SDK settings were persisted through NbPreferences.'

            $openProject = Invoke-NetBeansCli $Executable @(
                '--userdir', $userdir,
                '--cachedir', $hostHandle.Cachedir,
                '--nosplash',
                '--open', $lifecycleFixture.ProjectDirectory
            ) $CommandTimeout
            Assert-NetBeansCliStderr $openProject 'initial Flutter project open'
            Save-CommandEvidence $evidence 'open-flutter-project' $openProject
            Wait-FlutterLifecyclePersistence $userdir $lifecycleFixture $true `
                ([Math]::Min($CommandTimeout, 30))
            Write-Pass 'NetBeans persisted the isolated Flutter project as an open project.'

            Stop-NetBeansProbeHost $hostHandle
            Clear-IsolatedNetBeansUserdirLock $hostHandle
            $hostHandle = $null
            Assert-FlutterSdkPreferences $userdir $lifecycleFixture
            Assert-FlutterProjectReopenRecord $userdir $lifecycleFixture
            Write-Pass 'Flutter SDK settings and the standard NetBeans project reopen record remain persisted after shutdown.'

            $cleanupScenario = Invoke-IsolatedDisableCleanup $Executable `
                $ScenarioRoot $CurrentMetadata $currentCatalog $lifecycleFixture `
                $evidence $StartupTimeout $CommandTimeout
        }
    } finally {
        if ($null -ne $hostHandle) {
            Stop-NetBeansProbeHost $hostHandle
            Clear-IsolatedNetBeansUserdirLock $hostHandle
        }
    }
    Assert-FinalLogClean $userdir
    Write-Pass "$Name scenario passed. Evidence: $ScenarioRoot"
    return [pscustomobject]@{
        Name = $Name
        Root = $ScenarioRoot
        Userdir = $userdir
        CleanupUserdir = if ($null -eq $cleanupScenario) {
            $null
        } else {
            $cleanupScenario.Userdir
        }
    }
}

function Get-DefaultCurrentNbmPath {
    param([string]$Root)

    $pomPath = Join-Path $Root 'pom.xml'
    if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
        throw "Repository root does not contain pom.xml: $Root"
    }
    $pom = Read-SafeXmlFile $pomPath
    $versionNode = $pom.SelectSingleNode(
        "/*[local-name()='project']/*[local-name()='version']")
    if ($null -eq $versionNode -or [string]::IsNullOrWhiteSpace($versionNode.InnerText)) {
        throw 'Could not read the Maven version from the root pom.xml.'
    }
    return Join-Path $Root (
        "netbeans-plugin\target\netbeans-plugin-$($versionNode.InnerText.Trim()).nbm")
}

function Invoke-NetBeans30NbmSmoke {
    param(
        [string]$Root,
        [string]$NbHome,
        [string]$CurrentNbm,
        [string]$PreviousNbm,
        [string]$RequestedProbeRoot,
        [int]$StartupTimeout,
        [int]$CommandTimeout
    )

    $resolvedRoot = (Resolve-Path -LiteralPath $Root).Path
    if ([string]::IsNullOrWhiteSpace($NbHome)) {
        throw 'Specify -NetBeansHome or set NETBEANS_HOME to an Apache NetBeans 30 installation.'
    }
    $resolvedNetBeansHome = (Resolve-Path -LiteralPath $NbHome).Path
    $executable = Join-Path $resolvedNetBeansHome 'bin\netbeans64.exe'
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw "NetBeans 64-bit launcher does not exist: $executable"
    }
    if ([string]::IsNullOrWhiteSpace($CurrentNbm)) {
        $CurrentNbm = Get-DefaultCurrentNbmPath $resolvedRoot
    } elseif (-not [System.IO.Path]::IsPathRooted($CurrentNbm)) {
        $CurrentNbm = Join-Path $resolvedRoot $CurrentNbm
    }
    if (-not [string]::IsNullOrWhiteSpace($PreviousNbm) -and
            -not [System.IO.Path]::IsPathRooted($PreviousNbm)) {
        $PreviousNbm = Join-Path $resolvedRoot $PreviousNbm
    }

    $currentMetadata = Get-NbmMetadata $CurrentNbm
    if ($currentMetadata.CodeName -cne $ExpectedModuleCodeName) {
        throw "Current NBM code name is '$($currentMetadata.CodeName)'; expected '$ExpectedModuleCodeName'."
    }
    $previousMetadata = if ([string]::IsNullOrWhiteSpace($PreviousNbm)) {
        $null
    } else {
        Get-NbmMetadata $PreviousNbm
    }
    if ($null -ne $previousMetadata) {
        if ($previousMetadata.CodeName -cne $currentMetadata.CodeName) {
            throw "Previous NBM code name '$($previousMetadata.CodeName)' differs from current '$($currentMetadata.CodeName)'."
        }
        if ((Compare-NumericSpecificationVersion `
                    $previousMetadata.SpecificationVersion `
                    $currentMetadata.SpecificationVersion) -ge 0) {
            throw "Previous specification version $($previousMetadata.SpecificationVersion) must be lower than current $($currentMetadata.SpecificationVersion)."
        }
    }

    $runRoot = Resolve-IsolatedProbeRoot $resolvedRoot $RequestedProbeRoot
    $ownership = $null
    try {
        [void][System.IO.Directory]::CreateDirectory($runRoot)
        Assert-NoReparsePointAncestors $resolvedRoot $runRoot
        $ownership = New-ProbeRootOwnership $runRoot
        Assert-NoReparsePointAncestors $resolvedRoot $runRoot
        Write-Info "NetBeans home: $resolvedNetBeansHome"
        Write-Info "Current NBM: $($currentMetadata.Path)"
        Write-Info "Dedicated probe root: $runRoot"
        Write-Info 'The real NetBeans userdir and installation will not be modified.'

        $scenarios = [System.Collections.Generic.List[object]]::new()
        $clean = Invoke-SmokeScenario 'Clean install' $executable `
            (Join-Path $runRoot 'clean-install') $currentMetadata $null `
            $StartupTimeout $CommandTimeout
        $scenarios.Add($clean)
        if ($null -ne $previousMetadata) {
            Write-Info "Previous NBM: $($previousMetadata.Path)"
            $upgrade = Invoke-SmokeScenario 'Upgrade' $executable `
                (Join-Path $runRoot 'upgrade') $currentMetadata $previousMetadata `
                $StartupTimeout $CommandTimeout
            $scenarios.Add($upgrade)
        }

        Write-Pass 'NetBeans 30 isolated NBM smoke PASSED.'
        foreach ($scenario in $scenarios) {
            Write-Info "$($scenario.Name): $($scenario.Userdir)"
        }
        return $scenarios.ToArray()
    } finally {
        if ($null -ne $ownership) {
            $ownership.Stream.Dispose()
        }
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    try {
        [void](Invoke-NetBeans30NbmSmoke `
                $RepositoryRoot `
                $NetBeansHome `
                $CurrentNbmPath `
                $PreviousNbmPath `
                $ProbeRoot `
                $StartupTimeoutSeconds `
                $CommandTimeoutSeconds)
        exit 0
    } catch {
        $failure = "NetBeans 30 isolated NBM smoke FAILED: $($_.Exception.Message)"
        if (-not [string]::IsNullOrWhiteSpace($_.ScriptStackTrace)) {
            $failure += "`n$($_.ScriptStackTrace)"
        }
        Write-Error $failure
        exit 1
    }
}
