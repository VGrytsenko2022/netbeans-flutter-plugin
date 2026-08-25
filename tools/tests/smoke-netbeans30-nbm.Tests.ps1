$Runner = (Resolve-Path (Join-Path $PSScriptRoot '..\smoke-netbeans30-nbm.ps1')).Path
. $Runner

function Write-Utf8File {
    param(
        [string]$Path,
        [string]$Content
    )
    [void](New-Item -ItemType Directory -Path (Split-Path -Parent $Path) -Force)
    [System.IO.File]::WriteAllText(
        $Path,
        $Content,
        [System.Text.UTF8Encoding]::new($false))
}

function New-ZipFile {
    param(
        [string]$Path,
        [hashtable]$Entries
    )
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [void](New-Item -ItemType Directory -Path (Split-Path -Parent $Path) -Force)
    $archive = [System.IO.Compression.ZipFile]::Open(
        $Path,
        [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($name in $Entries.Keys) {
            $entry = $archive.CreateEntry($name)
            $stream = $entry.Open()
            try {
                $value = $Entries[$name]
                if ($value -is [byte[]]) {
                    $stream.Write($value, 0, $value.Length)
                } else {
                    $writer = [System.IO.StreamWriter]::new(
                        $stream, [System.Text.UTF8Encoding]::new($false), 1024, $true)
                    try {
                        $writer.Write([string]$value)
                    } finally {
                        $writer.Dispose()
                    }
                }
            } finally {
                $stream.Dispose()
            }
        }
    } finally {
        $archive.Dispose()
    }
}

function New-NbmFixture {
    param(
        [string]$Path,
        [string]$SpecificationVersion = '0.1.2',
        [string]$ImplementationVersion = '0.1.2-20260824'
    )

    $info = @"
<?xml version="1.0" encoding="UTF-8"?>
<module codenamebase="dev.flutter.netbeans.netbeans.plugin"
        distribution="fixture.nbm" downloadsize="0" license="license-id"
        needsrestart="false" releasedate="2026/08/24">
  <manifest OpenIDE-Module="dev.flutter.netbeans.netbeans.plugin"
            OpenIDE-Module-Name="Flutter and Dart Support"
            OpenIDE-Module-Specification-Version="$SpecificationVersion"
            OpenIDE-Module-Implementation-Version="$ImplementationVersion"/>
  <license name="license-id">Apache License Version 2.0</license>
</module>
"@
    $moduleJar = "$Path.module.jar"
    New-ZipFile $moduleJar @{
        'META-INF/MANIFEST.MF' = @"
Manifest-Version: 1.0
OpenIDE-Module: dev.flutter.netbeans.netbeans.plugin
OpenIDE-Module-Specification-Version: $SpecificationVersion
OpenIDE-Module-Implementation-Version: $ImplementationVersion

"@
    }
    $config = @"
<module name="dev.flutter.netbeans.netbeans.plugin">
  <param name="enabled">true</param>
  <param name="jar">modules/dev-flutter-netbeans-netbeans-plugin.jar</param>
</module>
"@
    $tracking = @"
<?xml version="1.0" encoding="UTF-8"?>
<module codename="dev.flutter.netbeans.netbeans.plugin">
  <module_version last="true" origin="installer"
                  specification_version="$SpecificationVersion">
    <file name="config/Modules/dev-flutter-netbeans-netbeans-plugin.xml"/>
    <file name="modules/dev-flutter-netbeans-netbeans-plugin.jar"/>
    <file name="modules/ext/dev.flutter.netbeans.netbeans-plugin/dependency.jar"/>
    <file name="update_tracking/dev-flutter-netbeans-netbeans-plugin.xml"/>
  </module_version>
</module>
"@
    New-ZipFile $Path @{
        'Info/info.xml' = $info
        'netbeans/config/Modules/dev-flutter-netbeans-netbeans-plugin.xml' = $config
        'netbeans/modules/dev-flutter-netbeans-netbeans-plugin.jar' = `
            [System.IO.File]::ReadAllBytes($moduleJar)
        'netbeans/modules/ext/dev.flutter.netbeans.netbeans-plugin/dependency.jar' = `
            [System.Text.Encoding]::UTF8.GetBytes("dependency-$ImplementationVersion")
        'netbeans/update_tracking/dev-flutter-netbeans-netbeans-plugin.xml' = $tracking
    }
    return $Path
}

function New-InstalledFixture {
    param(
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [string]$CatalogPath,
        [switch]$IncludeBackup
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($Metadata.Path)
    try {
        foreach ($entry in $archive.Entries) {
            if (-not $entry.FullName.StartsWith('netbeans/') -or
                    $entry.FullName.EndsWith('/')) {
                continue
            }
            $relativeName = $entry.FullName.Substring('netbeans/'.Length)
            $installedPath = Join-Path $Userdir ($relativeName -replace '/', '\')
            [void](New-Item -ItemType Directory -Path (
                    Split-Path -Parent $installedPath) -Force)
            $source = $entry.Open()
            $destination = [System.IO.File]::Create($installedPath)
            try {
                $source.CopyTo($destination)
            } finally {
                $destination.Dispose()
                $source.Dispose()
            }
        }
    } finally {
        $archive.Dispose()
    }
    if ($IncludeBackup) {
        foreach ($name in $Metadata.PayloadFiles.Keys) {
            $installedPath = Join-Path $Userdir ($name -replace '/', '\')
            $backupPath = Join-Path $Userdir (
                'update\backup\netbeans\' + ($name -replace '/', '\'))
            [void][System.IO.Directory]::CreateDirectory(
                (Split-Path -Parent $backupPath))
            [System.IO.File]::Copy($installedPath, $backupPath, $false)
        }
    }
    $origin = [System.Uri]::new($CatalogPath).AbsoluteUri
    $trackedFileXml = @($Metadata.PayloadFiles.Keys | Sort-Object | ForEach-Object {
            "    <file name=`"$_`"/>"
        }) -join "`r`n"
    Write-Utf8File (Join-Path $Userdir `
        'update_tracking\dev-flutter-netbeans-netbeans-plugin.xml') @"
<module codename="dev.flutter.netbeans.netbeans.plugin">
  <module_version last="false" origin="$origin"
                  specification_version="$($Metadata.SpecificationVersion)">
$trackedFileXml
  </module_version>
  <module_version last="true" origin="updater"
                  specification_version="$($Metadata.SpecificationVersion)">
$trackedFileXml
  </module_version>
</module>
"@
    Write-Utf8File (Join-Path $Userdir 'var\log\messages.log') @"
  Product Version         = Apache NetBeans IDE 30
INFO [org.netbeans.core.startup.NbEvents]: Turning on modules:
    dev.flutter.netbeans.netbeans.plugin [$($Metadata.SpecificationVersion) $($Metadata.ImplementationVersion) 202608241900]
"@
}

function ConvertTo-JavaPropertyValue {
    param([string]$Value)

    return $Value.Replace('\', '\\').Replace(':', '\:')
}

function Write-FlutterSdkPreferencesFixture {
    param(
        [string]$Userdir,
        [pscustomobject]$Fixture,
        [string]$FlutterHome = $Fixture.FlutterHome,
        [string]$DartHome = $Fixture.DartHome,
        [string]$UseBundledDart = 'false',
        [string]$DiscoveryVersion = '1'
    )

    Write-Utf8File (Join-Path $Userdir $FlutterPreferencesRelativePath) @"
flutter.sdk.home=$(ConvertTo-JavaPropertyValue $FlutterHome)
dart.sdk.useBundled=$UseBundledDart
dart.sdk.home=$(ConvertTo-JavaPropertyValue $DartHome)
sdk.discovery.version=$DiscoveryVersion
"@
}

function Write-FlutterProjectReopenFixture {
    param(
        [string]$Userdir,
        [pscustomobject]$Fixture,
        [string]$ProjectDirectory = $Fixture.ProjectDirectory,
        [string]$AdditionalProjectDirectory
    )

    $projectUrl = [System.Uri]::new(
        $ProjectDirectory.TrimEnd('\') + '\').AbsoluteUri
    $additional = if ([string]::IsNullOrWhiteSpace($AdditionalProjectDirectory)) {
        ''
    } else {
        $additionalUrl = [System.Uri]::new(
            $AdditionalProjectDirectory.TrimEnd('\') + '\').AbsoluteUri
        "`r`nopenProjectsURLs.1=$(ConvertTo-JavaPropertyValue $additionalUrl)"
    }
    Write-Utf8File (Join-Path $Userdir $ProjectUiPreferencesRelativePath) @"
openProjectsURLs.0=$(ConvertTo-JavaPropertyValue $projectUrl)$additional
openProjectsDisplayNames.0=flutter_reopen_probe
"@
}

function Set-InstalledModuleEnabledFixture {
    param(
        [string]$Userdir,
        [bool]$Enabled
    )

    $path = Join-Path $Userdir "config\Modules\$ModuleConfigName"
    $text = [System.IO.File]::ReadAllText($path)
    $replacement = if ($Enabled) { 'true' } else { 'false' }
    $updated = $text -replace (
        '(<param\s+name="enabled">)(?:true|false)(</param>)'), `
        "`${1}$replacement`${2}"
    $expectedPattern = '<param\s+name="enabled">{0}</param>' -f $replacement
    if ($updated -ceq $text -and
            $text -notmatch $expectedPattern) {
        throw "Fixture module config has no enabled parameter: $path"
    }
    Write-Utf8File $path $updated
}

function New-StoppedHostFixture {
    param(
        [string]$Userdir,
        [int]$ProcessId = 4401
    )

    return [pscustomobject]@{
        Process = [pscustomobject]@{ Id = $ProcessId }
        Userdir = $Userdir
    }
}

function Test-ScriptBlockThrows {
    param([scriptblock]$Action)

    try {
        & $Action | Out-Null
        return $false
    } catch {
        return $true
    }
}

Describe 'smoke-netbeans30-nbm.ps1 pure helpers' {
    It 'reads Java properties with escaped Windows path separators' {
        $path = Join-Path $TestDrive 'java-properties\plugin.properties'
        Write-Utf8File $path @'
# NetBeans stores Windows paths with Java-properties escaping.
flutter.sdk.home=G\:\\SDKs\\flutter
dart.sdk.home=C\:\\Program Files\\Dart\\dart-sdk
dart.sdk.useBundled=false
'@

        $properties = Read-JavaPropertiesFile $path

        $properties['flutter.sdk.home'] | Should Be 'G:\SDKs\flutter'
        $properties['dart.sdk.home'] | Should Be 'C:\Program Files\Dart\dart-sdk'
        $properties['dart.sdk.useBundled'] | Should Be 'false'
    }

    It 'creates deterministic offline Flutter lifecycle fixtures' {
        $first = New-FlutterLifecycleFixture (Join-Path $TestDrive 'fixture-one')
        $second = New-FlutterLifecycleFixture (Join-Path $TestDrive 'fixture-two')

        foreach ($fixture in @($first, $second)) {
            (Test-SameCanonicalPath $fixture.Root (
                    Split-Path -Parent $fixture.ProjectDirectory)) | Should Be $true
            (Test-Path -LiteralPath (Join-Path $fixture.ProjectDirectory `
                    'pubspec.yaml') -PathType Leaf) | Should Be $true
            (Test-Path -LiteralPath (Join-Path $fixture.ProjectDirectory `
                    'lib\main.dart') -PathType Leaf) | Should Be $true
            (Test-Path -LiteralPath (Join-Path $fixture.FlutterHome `
                    'bin\flutter.bat') -PathType Leaf) | Should Be $true
            (Test-Path -LiteralPath (Join-Path $fixture.DartHome `
                    'bin\dart.bat') -PathType Leaf) | Should Be $true
        }
        foreach ($relativePath in @(
                'pubspec.yaml',
                'lib\main.dart')) {
            (Get-FileHash -LiteralPath (Join-Path $first.ProjectDirectory `
                        $relativePath) -Algorithm SHA256).Hash | Should Be `
                (Get-FileHash -LiteralPath (Join-Path $second.ProjectDirectory `
                        $relativePath) -Algorithm SHA256).Hash
        }
        (Get-FileHash -LiteralPath (Join-Path $first.FlutterHome `
                    'bin\flutter.bat') -Algorithm SHA256).Hash | Should Be `
            (Get-FileHash -LiteralPath (Join-Path $second.FlutterHome `
                    'bin\flutter.bat') -Algorithm SHA256).Hash
        (Get-FileHash -LiteralPath (Join-Path $first.DartHome `
                    'bin\dart.bat') -Algorithm SHA256).Hash | Should Be `
            (Get-FileHash -LiteralPath (Join-Path $second.DartHome `
                    'bin\dart.bat') -Algorithm SHA256).Hash
        (Get-Content -LiteralPath (Join-Path $first.ProjectDirectory `
                    'pubspec.yaml') -Raw) | Should Match '(?m)^\s*flutter:\s*$'
        (Get-Content -LiteralPath (Join-Path $first.FlutterHome `
                    'bin\flutter.bat') -Raw) | Should Match '(?i)devices'
    }

    It 'requires exact persisted Flutter and standalone Dart SDK settings' {
        $fixture = New-FlutterLifecycleFixture (Join-Path $TestDrive `
            'sdk-preferences')
        $userdir = Join-Path $TestDrive 'sdk-preferences-userdir'
        Write-FlutterSdkPreferencesFixture $userdir $fixture

        Assert-FlutterSdkPreferences $userdir $fixture

        Write-FlutterSdkPreferencesFixture $userdir $fixture `
            -DartHome (Join-Path $fixture.Root 'sdk\other-dart')
        (Test-ScriptBlockThrows {
                Assert-FlutterSdkPreferences $userdir $fixture
            }) | Should Be $true
        Write-FlutterSdkPreferencesFixture $userdir $fixture `
            -UseBundledDart 'true'
        (Test-ScriptBlockThrows {
                Assert-FlutterSdkPreferences $userdir $fixture
            }) | Should Be $true
        Write-FlutterSdkPreferencesFixture $userdir $fixture `
            -DiscoveryVersion '0'
        (Test-ScriptBlockThrows {
                Assert-FlutterSdkPreferences $userdir $fixture
            }) | Should Be $true
    }

    It 'requires exactly the lifecycle fixture project reopen URL and name' {
        $fixture = New-FlutterLifecycleFixture (Join-Path $TestDrive `
            'project-reopen')
        $userdir = Join-Path $TestDrive 'project-reopen-userdir'
        Write-FlutterProjectReopenFixture $userdir $fixture

        Assert-FlutterProjectReopenRecord $userdir $fixture

        $otherProject = Join-Path $fixture.Root 'other-project'
        [void][System.IO.Directory]::CreateDirectory($otherProject)
        Write-FlutterProjectReopenFixture $userdir $fixture `
            -ProjectDirectory $otherProject
        (Test-ScriptBlockThrows {
                Assert-FlutterProjectReopenRecord $userdir $fixture
            }) | Should Be $true
        Write-FlutterProjectReopenFixture $userdir $fixture `
            -AdditionalProjectDirectory $otherProject
        (Test-ScriptBlockThrows {
                Assert-FlutterProjectReopenRecord $userdir $fixture
            }) | Should Be $true
    }

    It 'stages an immutable local catalog from the exact NBM bytes' {
        $nbm = New-NbmFixture (Join-Path $TestDrive `
            'artifact\netbeans-plugin-0.1.2-SNAPSHOT.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalog = New-LocalUpdateCatalog $metadata (Join-Path $TestDrive 'site')

        $metadata.CodeName | Should Be 'dev.flutter.netbeans.netbeans.plugin'
        $metadata.SpecificationVersion | Should Be '0.1.2'
        $metadata.PayloadFiles.Count | Should Be 4
        (Get-FileHash $nbm -Algorithm SHA256).Hash | Should Be `
            (Get-FileHash $catalog.NbmPath -Algorithm SHA256).Hash
        $document = Read-SafeXmlFile $catalog.Path
        $module = $document.SelectSingleNode(
            "/*[local-name()='module_updates']/*[local-name()='module']")
        $module.GetAttribute('distribution') | Should Be `
            'netbeans-plugin-0.1.2-SNAPSHOT.nbm'
        $module.GetAttribute('downloadsize') | Should Be `
            ([string](Get-Item $nbm).Length)
        $digest = $module.SelectSingleNode("./*[local-name()='message_digest']")
        $digest.GetAttribute('algorithm') | Should Be 'SHA-512'
        $digest.GetAttribute('value') | Should Be $catalog.Sha512
        @($module.SelectNodes("./*[local-name()='license']")).Count | Should Be 0
        @($document.SelectNodes(
                "/*[local-name()='module_updates']/*[local-name()='license']")).Count |
            Should Be 1
    }

    It 'accepts only new, target-descendant probe roots' {
        $repository = Join-Path $TestDrive 'repository'
        [void](New-Item -ItemType Directory -Path (Join-Path $repository 'target') -Force)
        $valid = Resolve-IsolatedProbeRoot $repository `
            (Join-Path $repository 'target\probe-1')

        $valid | Should Be (Join-Path $repository 'target\probe-1')
        try {
            [void](Resolve-IsolatedProbeRoot $repository $repository)
            $outsideRejected = $false
        } catch {
            $outsideRejected = $true
        }
        $outsideRejected | Should Be $true
        try {
            [void](Resolve-IsolatedProbeRoot $repository (Join-Path $repository 'target'))
            $targetRootRejected = $false
        } catch {
            $targetRootRejected = $true
        }
        $targetRootRejected | Should Be $true
        [void](New-Item -ItemType Directory -Path $valid)
        try {
            [void](Resolve-IsolatedProbeRoot $repository $valid)
            $existingRejected = $false
        } catch {
            $existingRejected = $true
        }
        $existingRejected | Should Be $true
    }

    It 'creates a literal probe root and holds exclusive ownership through orchestration' {
        $repository = Join-Path $TestDrive 'orchestration-repository'
        [void][System.IO.Directory]::CreateDirectory((Join-Path $repository 'target'))
        $netBeansHome = Join-Path $TestDrive 'fake-netbeans'
        $launcher = Join-Path $netBeansHome 'bin\netbeans64.exe'
        Write-Utf8File $launcher 'fixture launcher'
        $nbm = New-NbmFixture (Join-Path $TestDrive `
            'orchestration-artifact\netbeans-plugin-0.1.2-SNAPSHOT.nbm')
        $probeRoot = Join-Path $repository 'target\literal[1]'
        $script:observedScenarioRoot = $null
        $script:ownershipWasExclusive = $false
        Mock Invoke-SmokeScenario {
            param(
                $Name,
                $Executable,
                $ScenarioRoot,
                $CurrentMetadata,
                $PreviousMetadata,
                $StartupTimeout,
                $CommandTimeout
            )
            $script:observedScenarioRoot = $ScenarioRoot
            $ownerPath = Join-Path (Split-Path -Parent $ScenarioRoot) `
                '.netbeans30-smoke-owner'
            try {
                $competingStream = [System.IO.File]::Open(
                    $ownerPath,
                    [System.IO.FileMode]::Open,
                    [System.IO.FileAccess]::ReadWrite,
                    [System.IO.FileShare]::ReadWrite)
                $competingStream.Dispose()
            } catch [System.IO.IOException] {
                $script:ownershipWasExclusive = $true
            }
            return [pscustomobject]@{
                Name = $Name
                Root = $ScenarioRoot
                Userdir = Join-Path $ScenarioRoot 'userdir'
            }
        }

        [void](Invoke-NetBeans30NbmSmoke $repository $netBeansHome $nbm $null `
                $probeRoot 10 10)

        (Test-Path -LiteralPath $probeRoot -PathType Container) | Should Be $true
        $script:observedScenarioRoot | Should Be (Join-Path $probeRoot 'clean-install')
        $script:ownershipWasExclusive | Should Be $true
        $ownerPath = Join-Path $probeRoot '.netbeans30-smoke-owner'
        $releasedStream = [System.IO.File]::Open(
            $ownerPath,
            [System.IO.FileMode]::Open,
            [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None)
        $releasedStream.Dispose()
        Assert-MockCalled Invoke-SmokeScenario -Times 1 -Exactly
    }

    It 'atomically rejects a second owner for the same probe root' {
        $runRoot = Join-Path $TestDrive 'owned-root'
        [void][System.IO.Directory]::CreateDirectory($runRoot)
        $ownership = New-ProbeRootOwnership $runRoot
        try {
            try {
                [void](New-ProbeRootOwnership $runRoot)
                $secondOwnerRejected = $false
            } catch {
                $secondOwnerRejected = $true
            }
            $secondOwnerRejected | Should Be $true
        } finally {
            $ownership.Stream.Dispose()
        }
    }

    It 'rejects a target descendant routed through a junction' {
        $repository = Join-Path $TestDrive 'junction-repository'
        $target = Join-Path $repository 'target'
        $outside = Join-Path $TestDrive 'outside-target'
        [void](New-Item -ItemType Directory -Path $target, $outside -Force)
        $junction = Join-Path $target 'escape'
        [void](New-Item -ItemType Junction -Path $junction -Target $outside)

        ((Get-Item -LiteralPath $junction -Force).Attributes -band
                [System.IO.FileAttributes]::ReparsePoint) -ne 0 | Should Be $true
        try {
            [void](Resolve-IsolatedProbeRoot $repository (Join-Path $junction 'probe'))
            $junctionRejected = $false
        } catch {
            $junctionRejected = $true
        }
        $junctionRejected | Should Be $true
    }

    It 'compares dot-separated specification versions numerically' {
        (Compare-NumericSpecificationVersion '0.1.1' '0.1.2') | Should Be -1
        (Compare-NumericSpecificationVersion '0.10' '0.2') | Should Be 1
        (Compare-NumericSpecificationVersion '1.2.0' '1.2') | Should Be 0
        try {
            [void](Compare-NumericSpecificationVersion '1.2-SNAPSHOT' '1.2')
            $invalidRejected = $false
        } catch {
            $invalidRejected = $true
        }
        $invalidRejected | Should Be $true
    }

    It 'parses Enabled module list rows and detects critical log patterns' {
        $output = @"
Code Name                                          Version                                State
-------------------------------------------------- -------------------------------------- -------------
dev.flutter.netbeans.netbeans.plugin               0.1.2     Enabled
-------------------------------------------------- -------------------------------------- -------------
"@
        (Test-ModuleListReady $output) | Should Be $true
        $record = Get-ModuleListRecord $output `
            'dev.flutter.netbeans.netbeans.plugin'
        $record.Version | Should Be '0.1.2'
        $record.State | Should Be 'Enabled'
        @(Get-CriticalLogLines 'INFO clean').Count | Should Be 0
        @(Get-CriticalLogLines "INFO ok`nSEVERE broken").Count | Should Be 1
        (Test-NetBeans30ProductLog `
                'Product Version = Apache NetBeans IDE 30') | Should Be $true
        (Test-NetBeans30ProductLog `
                'Product Version = Apache NetBeans IDE 300') | Should Be $false
        (Test-NetBeans30ProductLog `
                'Product Version = Apache NetBeans IDE 31') | Should Be $false
        $metadata = [pscustomobject]@{
            CodeName = 'dev.flutter.netbeans.netbeans.plugin'
            SpecificationVersion = '0.1.2'
            ImplementationVersion = '0.1.2-20260824'
        }
        $activationLog = @"
  Product Version         = Apache NetBeans IDE 30
    dev.flutter.netbeans.netbeans.plugin [0.1.2 0.1.2-20260824 build]
"@
        (Test-NetBeansActivationLog $activationLog $metadata) | Should Be $true
    }

    It 'parses exact launcher and java userdir arguments without substring matches' {
        $userdir = Join-Path $TestDrive 'process probe\userdir'
        $launcher = '"G:\NetBeans 30\bin\netbeans64.exe" --userdir "' +
            $userdir + '" --cachedir "G:\cache"'
        $java = '"G:\Java\bin\javaw.exe" "-Dnetbeans.user=' + $userdir +
            '" "-XX:HeapDumpPath=' + $userdir + '\var\log\heapdump.hprof"'
        $substringOnly = '"G:\Java\bin\javaw.exe" "-XX:HeapDumpPath=' +
            $userdir + '\var\log\heapdump.hprof"'

        (Get-CommandLineUserdir $launcher) | Should Be (Get-CanonicalPath $userdir)
        (Get-CommandLineUserdir $java) | Should Be (Get-CanonicalPath $userdir)
        (Get-CommandLineUserdir $substringOnly) | Should Be $null
    }

    It 'selects only identity-validated descendants for process cleanup' {
        $userdir = Join-Path $TestDrive 'owned-process\userdir'
        $started = [System.DateTime]::UtcNow.AddSeconds(-5)
        $hostHandle = [pscustomobject]@{
            Process = [pscustomobject]@{ Id = 100 }
            Userdir = $userdir
            StartTimeUtc = $started
        }
        $owned = [pscustomobject]@{
            ProcessId = 101
            ParentProcessId = 100
            Name = 'javaw.exe'
            ExecutablePath = 'G:\Java\bin\javaw.exe'
            CreationDate = $started.AddSeconds(1)
            CommandLine = '"G:\Java\bin\javaw.exe" "-Dnetbeans.user=' +
                $userdir + '"'
        }
        $substringOnly = [pscustomobject]@{
            ProcessId = 102
            ParentProcessId = 100
            Name = 'javaw.exe'
            ExecutablePath = 'G:\Java\bin\javaw.exe'
            CreationDate = $started.AddSeconds(1)
            CommandLine = '"G:\Java\bin\javaw.exe" "-XX:HeapDumpPath=' +
                $userdir + '\heapdump.hprof"'
        }

        $selected = @(Select-OwnedNetBeansProbeProcesses $hostHandle @(
                $owned, $substringOnly))
        $selected.Count | Should Be 1
        $selected[0].ProcessId | Should Be 101

        $notDescendant = $owned.PSObject.Copy()
        $notDescendant.ParentProcessId = 999
        try {
            [void](Select-OwnedNetBeansProbeProcesses $hostHandle @($notDescendant))
            $unownedRejected = $false
        } catch {
            $unownedRejected = $true
        }
        $unownedRejected | Should Be $true
    }

    It 'kills through a retained identity-validated process handle' {
        $userdir = Join-Path $TestDrive 'retained-process\userdir'
        $started = [System.DateTime]::UtcNow.AddSeconds(-5)
        $hostHandle = [pscustomobject]@{
            Process = [pscustomobject]@{ Id = 100 }
            Userdir = $userdir
            StartTimeUtc = $started.AddSeconds(-1)
        }
        $expected = [pscustomobject]@{
            ProcessId = 101
            ParentProcessId = 100
            Name = 'javaw.exe'
            ExecutablePath = 'G:\Java\bin\javaw.exe'
            CreationTimeUtc = $started
        }
        $actual = [pscustomobject]@{
            ProcessId = 101
            ParentProcessId = 100
            Name = 'javaw.exe'
            ExecutablePath = 'G:\Java\bin\javaw.exe'
            CreationDate = $started
            CommandLine = '"G:\Java\bin\javaw.exe" "-Dnetbeans.user=' +
                $userdir + '"'
        }
        $script:retainedKilled = 0
        $script:retainedDisposed = 0
        $script:retainedProcess = [pscustomobject]@{
            HasExited = $false
            SafeHandle = [pscustomobject]@{ IsInvalid = $false; IsClosed = $false }
            MainModule = [pscustomobject]@{ FileName = 'G:\Java\bin\javaw.exe' }
            StartTime = $started
        }
        $script:retainedProcess | Add-Member ScriptMethod Refresh {}
        $script:retainedProcess | Add-Member ScriptMethod Kill {
            param($EntireProcessTree)
            $script:retainedKilled++
            $this.HasExited = $true
        }
        $script:retainedProcess | Add-Member ScriptMethod WaitForExit {
            param($Milliseconds)
            return $true
        }
        $script:retainedProcess | Add-Member ScriptMethod Dispose {
            $script:retainedDisposed++
        }
        $openProcess = { param($ProcessId) return $script:retainedProcess }

        (Stop-RetainedProbeProcess $expected $actual $hostHandle $openProcess) |
            Should Be $true

        $script:retainedKilled | Should Be 1
        $script:retainedDisposed | Should Be 1
    }

    It 'refuses a reused retained child identity and an occupied host PID' {
        $userdir = Join-Path $TestDrive 'reused-process\userdir'
        $started = [System.DateTime]::UtcNow.AddSeconds(-5)
        $hostHandle = [pscustomobject]@{
            Process = [pscustomobject]@{ Id = 100 }
            Userdir = $userdir
            StartTimeUtc = $started.AddSeconds(-1)
        }
        $expected = [pscustomobject]@{
            ProcessId = 101
            ParentProcessId = 100
            Name = 'javaw.exe'
            ExecutablePath = 'G:\Java\bin\javaw.exe'
            CreationTimeUtc = $started
        }
        $actual = [pscustomobject]@{
            ProcessId = 101
            ParentProcessId = 100
            Name = 'javaw.exe'
            ExecutablePath = 'G:\Java\bin\javaw.exe'
            CreationDate = $started
            CommandLine = '"G:\Java\bin\javaw.exe" "-Dnetbeans.user=' +
                $userdir + '"'
        }
        $script:replacementKilled = 0
        $script:replacement = [pscustomobject]@{
            HasExited = $false
            SafeHandle = [pscustomobject]@{ IsInvalid = $false; IsClosed = $false }
            MainModule = [pscustomobject]@{ FileName = 'G:\Java\bin\javaw.exe' }
            StartTime = $started.AddSeconds(2)
        }
        $script:replacement | Add-Member ScriptMethod Refresh {}
        $script:replacement | Add-Member ScriptMethod Kill {
            param($EntireProcessTree)
            $script:replacementKilled++
        }
        $script:replacement | Add-Member ScriptMethod WaitForExit {
            param($Milliseconds)
            return $true
        }
        $script:replacement | Add-Member ScriptMethod Dispose {}
        $openReplacement = { param($ProcessId) return $script:replacement }

        try {
            [void](Stop-RetainedProbeProcess $expected $actual $hostHandle `
                    $openReplacement)
            $replacementRejected = $false
        } catch {
            $replacementRejected = $true
        }
        $replacementRejected | Should Be $true
        $script:replacementKilled | Should Be 0

        $occupiedLookup = { param($ProcessId) return [pscustomobject]@{ Id = $ProcessId } }
        try {
            Assert-HostProcessIdVacant $hostHandle $occupiedLookup
            $occupiedRejected = $false
        } catch {
            $occupiedRejected = $true
        }
        $occupiedRejected | Should Be $true
        { Assert-HostProcessIdVacant $hostHandle { param($ProcessId) return @() } } |
            Should Not Throw
    }

    It 'accepts a validated Java child after the Windows launcher exits' {
        $launcher = [pscustomobject]@{ Id = 100; HasExited = $true; ExitCode = -252 }
        $launcher | Add-Member -MemberType ScriptMethod -Name Refresh -Value {}
        $hostHandle = [pscustomobject]@{ Process = $launcher }
        Mock Assert-HostProcessIdVacant {}
        Mock Get-NetBeansProbeProcesses {
            return @([pscustomobject]@{ ProcessId = 101; Name = 'javaw.exe' })
        }

        (Test-NetBeansProbeAlive $hostHandle) | Should Be $true

        Assert-MockCalled Assert-HostProcessIdVacant -Times 1 -Exactly
        Assert-MockCalled Get-NetBeansProbeProcesses -Times 1 -Exactly
    }

    It 'clears only an exclusively available lock in a stopped isolated userdir' {
        $userdir = Join-Path $TestDrive 'stale-lock\userdir'
        $lockPath = Join-Path $userdir 'lock'
        Write-Utf8File $lockPath 'stale fixture lock'
        $launcher = [pscustomobject]@{ Id = 100; HasExited = $true; ExitCode = -252 }
        $launcher | Add-Member -MemberType ScriptMethod -Name Refresh -Value {}
        $hostHandle = [pscustomobject]@{
            Process = $launcher
            Userdir = $userdir
        }
        Mock Assert-HostProcessIdVacant {}
        Mock Get-NetBeansProbeProcesses { return @() }

        { Clear-IsolatedNetBeansUserdirLock $hostHandle } | Should Not Throw

        (Test-Path -LiteralPath $lockPath) | Should Be $false
    }

    It 'rejects meaningful CLI stderr and allows only explicit harmless lines' {
        @(Get-UnexpectedCliStderrLines `
                'WARNING: package com.sun.tools.classfile not in jdk.jdeps').Count |
            Should Be 0
        @(Get-UnexpectedCliStderrLines `
                'WARNING: package com.apple.laf not in java.desktop').Count |
            Should Be 0
        @(Get-UnexpectedCliStderrLines `
                'WARNING: package com.apple.eio not in java.desktop').Count |
            Should Be 1
        $failed = [pscustomobject]@{
            ExitCode = -252
            StandardOutput = 'Installing module'
            StandardError = 'CommandException: install failed'
        }
        try {
            Assert-NetBeansCliStderr $failed 'fixture operation'
            $stderrRejected = $false
        } catch {
            $stderrRejected = $true
        }
        $stderrRejected | Should Be $true
    }

    It 'recognizes a published NetBeans CLI endpoint only after its four-byte port marker' {
        $userdir = Join-Path $TestDrive 'endpoint-marker\userdir'
        [void][System.IO.Directory]::CreateDirectory($userdir)
        (Test-IsolatedNetBeansCliEndpointPublished $userdir) | Should Be $false
        $lockPath = Join-Path $userdir 'lock'
        [System.IO.File]::WriteAllBytes($lockPath, [byte[]](1, 2, 3))
        (Test-IsolatedNetBeansCliEndpointPublished $userdir) | Should Be $false
        [System.IO.File]::WriteAllBytes($lockPath, [byte[]](1, 2, 3, 4))
        (Test-IsolatedNetBeansCliEndpointPublished $userdir) | Should Be $true
    }

    It 'retries an inner CLI timeout and passes the same isolated cachedir' {
        $fakeProcess = [pscustomobject]@{
            HasExited = $false
            ExitCode = 0
        }
        $fakeProcess | Add-Member -MemberType ScriptMethod -Name Refresh -Value {}
        $cachedir = Join-Path $TestDrive 'retry\cache'
        $hostHandle = [pscustomobject]@{
            Process = $fakeProcess
            Cachedir = $cachedir
        }
        $script:cliAttempts = 0
        $script:observedArguments = $null
        Mock Invoke-NetBeansCli {
            param($Executable, $Arguments, $TimeoutSeconds)
            $script:cliAttempts++
            $script:observedArguments = $Arguments
            if ($script:cliAttempts -eq 1) {
                throw [System.TimeoutException]::new('transient timeout')
            }
            return [pscustomobject]@{
                ExitCode = -252
                StandardOutput = @"
Code Name Version State
---------- ---------- -----
"@
                StandardError = ''
            }
        }
        Mock Test-IsolatedNetBeansCliEndpointPublished { return $true }

        [void](Wait-NetBeansCliReady $hostHandle 'netbeans64.exe' `
                (Join-Path $TestDrive 'retry\userdir') 10 10)

        $script:cliAttempts | Should Be 2
        $cacheIndex = [Array]::IndexOf($script:observedArguments, '--cachedir')
        $cacheIndex | Should BeGreaterThan -1
        $script:observedArguments[$cacheIndex + 1] | Should Be $cachedir
    }

    It 'waits for primary userdir ownership before starting the readiness CLI' {
        $fakeProcess = [pscustomobject]@{
            HasExited = $false
            ExitCode = 0
        }
        $fakeProcess | Add-Member -MemberType ScriptMethod -Name Refresh -Value {}
        $hostHandle = [pscustomobject]@{
            Process = $fakeProcess
            Cachedir = Join-Path $TestDrive 'ownership\cache'
        }
        $script:lockChecks = 0
        $script:readinessCalls = 0
        Mock Start-Sleep {}
        Mock Test-IsolatedNetBeansCliEndpointPublished {
            $script:lockChecks++
            return $script:lockChecks -ge 3
        }
        Mock Invoke-NetBeansCli {
            $script:readinessCalls++
            return [pscustomobject]@{
                ExitCode = -252
                StandardOutput = "Code Name Version State`r`n---------- ---------- -----"
                StandardError = ''
            }
        }

        [void](Wait-NetBeansCliReady $hostHandle 'netbeans64.exe' `
                (Join-Path $TestDrive 'ownership\userdir') 10 10)

        $script:lockChecks | Should Be 3
        $script:readinessCalls | Should Be 1
    }

    It 'restarts and cleans an isolated host that exits during readiness' {
        $script:hostStarts = 0
        $script:hostCleanups = 0
        Mock Start-Sleep {}
        Mock Start-NetBeansProbeHost {
            $script:hostStarts++
            return [pscustomobject]@{
                Process = [pscustomobject]@{ Id = 100 + $script:hostStarts }
                Userdir = 'fixture-userdir'
                Cachedir = 'fixture-cachedir'
            }
        }
        Mock Wait-NetBeansCliReady {
            if ($script:hostStarts -eq 1) {
                throw 'fixture launcher handoff'
            }
            return [pscustomobject]@{
                ExitCode = -252
                StandardOutput = "Code Name Version State`r`n---------- ---------- -----"
                StandardError = ''
            }
        }
        Mock Stop-NetBeansProbeHost { $script:hostCleanups++ }
        Mock Clear-IsolatedNetBeansUserdirLock {}

        $started = Start-ReadyNetBeansProbe 'netbeans64.exe' `
            'fixture-userdir' 'fixture-cachedir' 10 10

        $started.Attempt | Should Be 2
        $started.HostHandle.Process.Id | Should Be 102
        $script:hostStarts | Should Be 2
        $script:hostCleanups | Should Be 1
    }

    It 'handles exact module states and a direct disable with empty stdout' {
        $metadata = [pscustomobject]@{
            CodeName = 'dev.flutter.netbeans.netbeans.plugin'
            SpecificationVersion = '0.1.2'
        }
        $hostHandle = [pscustomobject]@{
            Cachedir = 'fixture-cache'
        }
        $script:reportedModuleState = 'Enabled'
        $script:stateAliveChecks = 0
        $script:stopAfterFirstAlive = $false
        $script:listCliCalls = 0
        $script:disableCalls = 0
        $script:disableArguments = $null
        $script:disableTimeout = 0
        Mock Start-Sleep {}
        Mock Test-NetBeansProbeAlive {
            if (-not $script:stopAfterFirstAlive) {
                return $true
            }
            $script:stateAliveChecks++
            return $script:stateAliveChecks -eq 1
        }
        Mock Invoke-NetBeansCli {
            param($Executable, $Arguments, $TimeoutSeconds)
            if ([Array]::IndexOf($Arguments, '--direct-disable') -ge 0) {
                $script:disableCalls++
                $script:disableArguments = @($Arguments)
                $script:disableTimeout = $TimeoutSeconds
                return [pscustomobject]@{
                    ExitCode = -252
                    StandardOutput = ''
                    StandardError = ''
                }
            }
            $script:listCliCalls++
            return [pscustomobject]@{
                ExitCode = -252
                StandardOutput = @"
Code Name                                          Version State
-------------------------------------------------- ------- ---------
dev.flutter.netbeans.netbeans.plugin               0.1.2   $script:reportedModuleState
"@
                StandardError = ''
            }
        }

        $enabled = Wait-ModuleState $hostHandle 'netbeans64.exe' `
            'fixture-userdir' $metadata 'Enabled' 10 10
        $enabled.Record.Version | Should Be '0.1.2'
        $enabled.Record.State | Should Be 'Enabled'

        $script:reportedModuleState = 'Installed'
        $installed = Wait-ModuleState $hostHandle 'netbeans64.exe' `
            'fixture-userdir' $metadata 'Installed' 10 10
        $installed.Record.Version | Should Be '0.1.2'
        $installed.Record.State | Should Be 'Installed'

        $script:reportedModuleState = 'Installed'
        $script:stateAliveChecks = 0
        $script:stopAfterFirstAlive = $true
        try {
            [void](Wait-ModuleState $hostHandle 'netbeans64.exe' `
                'fixture-userdir' $metadata 'Enabled' 10 10)
            $stateMismatchRejected = $false
        } catch {
            $stateMismatchRejected = $true
        }
        $stateMismatchRejected | Should Be $true
        $script:stopAfterFirstAlive = $false

        $result = Invoke-DirectDisableModule 'netbeans64.exe' `
            'fixture-userdir' 'fixture-cache' $metadata 17

        $result.StandardOutput | Should Be ''
        $script:listCliCalls | Should Be 3
        $script:disableCalls | Should Be 1
        $script:disableTimeout | Should Be 17
        ($script:disableArguments -join '|') | Should Be (@(
                '--userdir', 'fixture-userdir',
                '--cachedir', 'fixture-cache',
                '--nosplash', '--modules', '--direct-disable',
                '^dev\.flutter\.netbeans\.netbeans\.plugin$'
            ) -join '|')
    }

    It 'requires every unrelated module version and state to remain unchanged' {
        $before = @"
Code Name Version State
---------- ------- -----
dev.flutter.netbeans.netbeans.plugin 0.1.2 Enabled
unrelated.module 7.4 Enabled
"@
        $validAfter = @"
Code Name Version State
---------- ------- -----
dev.flutter.netbeans.netbeans.plugin 0.1.2 Installed
unrelated.module 7.4 Enabled
"@
        $changedAfter = $validAfter.Replace(
            'unrelated.module 7.4 Enabled',
            'unrelated.module 7.4 Installed')

        { Assert-UnrelatedModuleStatesEqual $before $validAfter `
                'dev.flutter.netbeans.netbeans.plugin' } | Should Not Throw
        (Test-ScriptBlockThrows {
                Assert-UnrelatedModuleStatesEqual $before $changedAfter `
                    'dev.flutter.netbeans.netbeans.plugin'
            }) | Should Be $true
    }

    It 'retries a transient empty install response only after checking module state' {
        $script:operationAttempts = 0
        $script:stateChecks = 0
        Mock Start-Sleep {}
        Mock Invoke-NetBeansCli {
            param($Executable, $Arguments, $TimeoutSeconds)
            if ([Array]::IndexOf($Arguments, '--list') -ge 0) {
                $script:stateChecks++
                return [pscustomobject]@{
                    ExitCode = -252
                    StandardOutput = "Code Name Version State`r`n---------- ---------- -----"
                    StandardError = ''
                }
            }
            $script:operationAttempts++
            return [pscustomobject]@{
                ExitCode = -252
                StandardOutput = if ($script:operationAttempts -eq 1) {
                    ''
                } else {
                    'Installing dev.flutter.netbeans.netbeans.plugin@0.1.1'
                }
                StandardError = ''
            }
        }
        $metadata = [pscustomobject]@{
            CodeName = 'dev.flutter.netbeans.netbeans.plugin'
            SpecificationVersion = '0.1.1'
        }
        $catalog = [pscustomobject]@{ Uri = 'file:///fixture/updates.xml' }

        $result = Invoke-InstallOrUpdate 'install' 'netbeans64.exe' `
            'fixture-userdir' 'fixture-cachedir' $catalog $metadata 10

        $result.StandardOutput | Should Match '^Installing '
        $script:operationAttempts | Should Be 2
        $script:stateChecks | Should Be 1
    }

    It 'retries only the exact transient local-catalog miss' {
        $script:catalogInstallAttempts = 0
        Mock Start-Sleep {}
        Mock Invoke-NetBeansCli {
            $script:catalogInstallAttempts++
            return [pscustomobject]@{
                ExitCode = -252
                StandardOutput = if ($script:catalogInstallAttempts -eq 1) {
                    'Refreshing file:///fixture/updates.xml'
                } else {
                    'Installing dev.flutter.netbeans.netbeans.plugin@0.1.2'
                }
                StandardError = if ($script:catalogInstallAttempts -eq 1) {
                    'Cannot find any module matching [^dev\.flutter\.netbeans\.netbeans\.plugin$]'
                } else {
                    ''
                }
            }
        }
        $metadata = [pscustomobject]@{
            CodeName = 'dev.flutter.netbeans.netbeans.plugin'
            SpecificationVersion = '0.1.2'
        }
        $catalog = [pscustomobject]@{ Uri = 'file:///fixture/updates.xml' }

        $result = Invoke-InstallOrUpdate 'install' 'netbeans64.exe' `
            'fixture-userdir' 'fixture-cachedir' $catalog $metadata 10

        $result.StandardOutput | Should Match '^Installing '
        $script:catalogInstallAttempts | Should Be 2
    }

    It 'defers a timed-out update to exact post-restart verification' {
        $script:updateCliCalls = 0
        Mock Invoke-NetBeansCli {
            $script:updateCliCalls++
            throw [System.TimeoutException]::new('fixture restart handoff')
        }
        $metadata = [pscustomobject]@{
            CodeName = 'dev.flutter.netbeans.netbeans.plugin'
            SpecificationVersion = '0.1.2'
        }
        $catalog = [pscustomobject]@{ Uri = 'file:///fixture/updates.xml' }

        $result = Invoke-InstallOrUpdate 'update' 'netbeans64.exe' `
            'fixture-userdir' 'fixture-cachedir' $catalog $metadata 10

        $result.TimedOut | Should Be $true
        $result.StandardOutput | Should Match 'post-restart verification'
        $script:updateCliCalls | Should Be 1
    }

    It 'waits until the NetBeans 30 product and module activation log is complete' {
        $userdir = Join-Path $TestDrive 'activation-log\userdir'
        $logPath = Join-Path $userdir 'var\log\messages.log'
        Write-Utf8File $logPath 'initializing'
        $fakeProcess = [pscustomobject]@{ HasExited = $false; ExitCode = 0 }
        $fakeProcess | Add-Member -MemberType ScriptMethod -Name Refresh -Value {}
        $hostHandle = [pscustomobject]@{ Process = $fakeProcess }
        $metadata = [pscustomobject]@{
            CodeName = 'dev.flutter.netbeans.netbeans.plugin'
            SpecificationVersion = '0.1.2'
            ImplementationVersion = '0.1.2-20260824'
        }
        $script:activationReads = 0
        Mock Start-Sleep {}
        Mock Read-SharedTextFile {
            $script:activationReads++
            if ($script:activationReads -eq 1) {
                return 'INFO startup still incomplete'
            }
            return @"
  Product Version         = Apache NetBeans IDE 30
    dev.flutter.netbeans.netbeans.plugin [0.1.2 0.1.2-20260824 build]
"@
        }

        { Wait-NetBeansActivationLog $hostHandle $userdir $metadata 10 } |
            Should Not Throw
        $script:activationReads | Should Be 2
    }

    It 'masks remote/default update centers only inside the probe userdir' {
        $userdir = Join-Path $TestDrive 'masked-userdir'

        Disable-DefaultUpdateCenters $userdir

        $providerDirectory = Join-Path $userdir 'config\Services\AutoupdateType'
        @(Get-ChildItem -LiteralPath $providerDirectory -File).Name | Should Be @(
            '3rdparty.instance_hidden',
            'distribution-update-provider.instance_hidden',
            'pluginportal-update-provider.instance_hidden'
        )
        @(Get-ChildItem -LiteralPath $providerDirectory -File |
                Where-Object Length -ne 0).Count | Should Be 0
    }

    It 'accepts NetBeans updater as last origin when the staged catalog is tracked' {
        $nbm = New-NbmFixture (Join-Path $TestDrive 'installed\fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $TestDrive 'installed\site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $TestDrive 'installed\userdir'
        New-InstalledFixture $userdir $metadata $catalogPath

        { Assert-InstalledModuleFiles $userdir $metadata $catalogPath } |
            Should Not Throw
        { Assert-FinalLogClean $userdir } | Should Not Throw
    }

    It 'accepts an exact disabled config as Installed while preserving payload bytes' {
        $nbm = New-NbmFixture (Join-Path $TestDrive `
            'disabled-installed\fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $TestDrive `
            'disabled-installed\site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $TestDrive 'disabled-installed\userdir'
        New-InstalledFixture $userdir $metadata $catalogPath
        Set-InstalledModuleEnabledFixture $userdir $false

        Assert-InstalledModuleFiles $userdir $metadata $catalogPath $null `
            -RequireActivation $false -ExpectedState 'Installed'
        (Test-ScriptBlockThrows {
                Assert-InstalledModuleFiles $userdir $metadata $catalogPath `
                    $null -RequireActivation $false -ExpectedState 'Enabled'
            }) | Should Be $true

        foreach ($name in $metadata.PayloadFiles.Keys | Where-Object {
                $_ -ine "config/Modules/$ModuleConfigName" -and
                $_ -ine "update_tracking/$ModuleConfigName"
            }) {
            $installedPath = Join-Path $userdir ($name -replace '/', '\')
            (Get-FileHash -LiteralPath $installedPath -Algorithm SHA256).Hash |
                Should Be $metadata.PayloadFiles[$name].Sha256
        }
    }

    It 'removes only exact tracked payload, backup copies, and tracking' {
        $scenario = Join-Path $TestDrive 'offline-remove-success'
        [void][System.IO.Directory]::CreateDirectory($scenario)
        $nbm = New-NbmFixture (Join-Path $scenario 'fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $scenario 'site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $scenario 'userdir'
        New-InstalledFixture $userdir $metadata $catalogPath -IncludeBackup
        Set-InstalledModuleEnabledFixture $userdir $false

        $preferencesPath = Join-Path $userdir $FlutterPreferencesRelativePath
        Write-Utf8File $preferencesPath 'flutter.sdk.home=G\:\\preserved'
        $userSentinel = Join-Path $userdir 'preserve.sentinel'
        Write-Utf8File $userSentinel 'preserve-userdir'
        $ownerSentinel = Join-Path $scenario '.netbeans30-smoke-owner'
        Write-Utf8File $ownerSentinel 'preserve-owner'

        $expectedRemoved = [System.Collections.Generic.List[string]]::new()
        foreach ($name in $metadata.PayloadFiles.Keys) {
            $expectedRemoved.Add((Get-CanonicalPath (
                Join-Path $userdir ($name -replace '/', '\'))))
            $expectedRemoved.Add((Get-CanonicalPath (Join-Path $userdir (
                'update\backup\netbeans\' + ($name -replace '/', '\')))))
        }
        $stoppedHost = New-StoppedHostFixture $userdir
        Mock Test-NetBeansProbeAlive { return $false }
        Mock Get-NetBeansProbeProcesses { return @() }

        $result = Remove-IsolatedInstalledModule $scenario $userdir `
            $metadata $catalogPath $stoppedHost

        (($result.RemovedPaths | Sort-Object) -join '|') | Should Be `
            (($expectedRemoved.ToArray() | Sort-Object) -join '|')
        foreach ($removedPath in $expectedRemoved) {
            (Test-Path -LiteralPath $removedPath) | Should Be $false
        }
        (Get-Content -LiteralPath $preferencesPath -Raw) |
            Should Be 'flutter.sdk.home=G\:\\preserved'
        (Get-Content -LiteralPath $userSentinel -Raw) |
            Should Be 'preserve-userdir'
        (Get-Content -LiteralPath $ownerSentinel -Raw) |
            Should Be 'preserve-owner'
        (Test-Path -LiteralPath $nbm -PathType Leaf) | Should Be $true
        (Test-Path -LiteralPath $catalogPath -PathType Leaf) | Should Be $true
    }

    It 'refuses offline removal while the installed module config is enabled' {
        $scenario = Join-Path $TestDrive 'offline-remove-enabled'
        [void][System.IO.Directory]::CreateDirectory($scenario)
        $nbm = New-NbmFixture (Join-Path $scenario 'fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $scenario 'site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $scenario 'userdir'
        New-InstalledFixture $userdir $metadata $catalogPath -IncludeBackup
        $stoppedHost = New-StoppedHostFixture $userdir 4402
        Mock Test-NetBeansProbeAlive { return $false }
        Mock Get-NetBeansProbeProcesses { return @() }

        (Test-ScriptBlockThrows {
                Remove-IsolatedInstalledModule $scenario $userdir $metadata `
                    $catalogPath $stoppedHost
            }) | Should Be $true

        foreach ($name in $metadata.PayloadFiles.Keys) {
            (Test-Path -LiteralPath (Join-Path $userdir (
                        $name -replace '/', '\')) -PathType Leaf) |
                Should Be $true
        }
        (Test-Path -LiteralPath (Join-Path $userdir `
                "update_tracking\$ModuleConfigName") -PathType Leaf) |
            Should Be $true
    }

    It 'refuses offline removal when an exact backup payload was tampered' {
        $scenario = Join-Path $TestDrive 'offline-remove-tampered'
        [void][System.IO.Directory]::CreateDirectory($scenario)
        $nbm = New-NbmFixture (Join-Path $scenario 'fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $scenario 'site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $scenario 'userdir'
        New-InstalledFixture $userdir $metadata $catalogPath -IncludeBackup
        Set-InstalledModuleEnabledFixture $userdir $false
        $stoppedHost = New-StoppedHostFixture $userdir 4403
        Mock Test-NetBeansProbeAlive { return $false }
        Mock Get-NetBeansProbeProcesses { return @() }
        $tamperedBackup = Join-Path $userdir (
            'update\backup\netbeans\modules\ext\' +
            'dev.flutter.netbeans.netbeans-plugin\dependency.jar')
        Write-Utf8File $tamperedBackup 'tampered-backup'

        (Test-ScriptBlockThrows {
                Remove-IsolatedInstalledModule $scenario $userdir $metadata `
                    $catalogPath $stoppedHost
            }) | Should Be $true

        foreach ($name in $metadata.PayloadFiles.Keys) {
            (Test-Path -LiteralPath (Join-Path $userdir (
                        $name -replace '/', '\')) -PathType Leaf) |
                Should Be $true
        }
    }

    It 'rejects unsafe tracking before removing any isolated file' {
        $scenario = Join-Path $TestDrive 'offline-remove-unsafe'
        [void][System.IO.Directory]::CreateDirectory($scenario)
        $nbm = New-NbmFixture (Join-Path $scenario 'fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $scenario 'site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $scenario 'userdir'
        New-InstalledFixture $userdir $metadata $catalogPath -IncludeBackup
        Set-InstalledModuleEnabledFixture $userdir $false
        $stoppedHost = New-StoppedHostFixture $userdir 4404
        Mock Test-NetBeansProbeAlive { return $false }
        Mock Get-NetBeansProbeProcesses { return @() }
        $outside = Join-Path $scenario 'outside.txt'
        Write-Utf8File $outside 'must-not-be-removed'
        $trackingPath = Join-Path $userdir `
            "update_tracking\$ModuleConfigName"
        $tracking = [System.IO.File]::ReadAllText($trackingPath)
        $tracking = $tracking.Replace(
            'modules/ext/dev.flutter.netbeans.netbeans-plugin/dependency.jar',
            '../outside.txt')
        Write-Utf8File $trackingPath $tracking

        $errorMessage = ''
        try {
            Remove-IsolatedInstalledModule $scenario $userdir $metadata `
                $catalogPath $stoppedHost
        } catch {
            $errorMessage = $_.Exception.Message
        }
        $errorMessage | Should Be `
            "Update tracking contains unsafe file path '../outside.txt'."

        (Get-Content -LiteralPath $outside -Raw) |
            Should Be 'must-not-be-removed'
        foreach ($name in $metadata.PayloadFiles.Keys) {
            (Test-Path -LiteralPath (Join-Path $userdir (
                        $name -replace '/', '\')) -PathType Leaf) |
                Should Be $true
        }
    }

    It 'requires the stopped host proof before offline removal' {
        $scenario = Join-Path $TestDrive 'offline-remove-no-host-proof'
        [void][System.IO.Directory]::CreateDirectory($scenario)
        $nbm = New-NbmFixture (Join-Path $scenario 'fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $scenario 'site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $scenario 'userdir'
        New-InstalledFixture $userdir $metadata $catalogPath -IncludeBackup
        Set-InstalledModuleEnabledFixture $userdir $false

        $errorMessage = ''
        try {
            Remove-IsolatedInstalledModule $scenario $userdir $metadata `
                $catalogPath $null
        } catch {
            $errorMessage = $_.Exception.Message
        }
        $errorMessage | Should Be `
            'Offline module cleanup requires the identity-validated handle of the stopped isolated NetBeans host.'
        foreach ($name in $metadata.PayloadFiles.Keys) {
            (Test-Path -LiteralPath (Join-Path $userdir (
                        $name -replace '/', '\')) -PathType Leaf) |
                Should Be $true
        }
    }

    It 'rejects malformed self-tracking backup before removing payload' {
        $scenario = Join-Path $TestDrive 'offline-remove-bad-tracking-backup'
        [void][System.IO.Directory]::CreateDirectory($scenario)
        $nbm = New-NbmFixture (Join-Path $scenario 'fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $scenario 'site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $scenario 'userdir'
        New-InstalledFixture $userdir $metadata $catalogPath -IncludeBackup
        Set-InstalledModuleEnabledFixture $userdir $false
        $stoppedHost = New-StoppedHostFixture $userdir 4405
        Mock Test-NetBeansProbeAlive { return $false }
        Mock Get-NetBeansProbeProcesses { return @() }
        $backupTracking = Join-Path $userdir (
            "update\backup\netbeans\update_tracking\$ModuleConfigName")
        $backupText = [System.IO.File]::ReadAllText($backupTracking).Replace(
            'codename="dev.flutter.netbeans.netbeans.plugin"',
            'codename="wrong.module"')
        Write-Utf8File $backupTracking $backupText

        $errorMessage = ''
        try {
            Remove-IsolatedInstalledModule $scenario $userdir $metadata `
                $catalogPath $stoppedHost
        } catch {
            $errorMessage = $_.Exception.Message
        }
        $errorMessage | Should Be `
            "Flutter backup update tracking is not the exact installed module layout: $backupTracking"
        foreach ($name in $metadata.PayloadFiles.Keys) {
            (Test-Path -LiteralPath (Join-Path $userdir (
                        $name -replace '/', '\')) -PathType Leaf) |
                Should Be $true
        }
    }

    It 'allows offline upgrade payload verification without claiming activation' {
        Mock Read-SharedTextFile {
            param($Path)
            return [System.IO.File]::ReadAllText($Path)
        }
        $nbm = New-NbmFixture (Join-Path $TestDrive 'offline\fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $TestDrive 'offline\site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $TestDrive 'offline\userdir'
        New-InstalledFixture $userdir $metadata $catalogPath
        Write-Utf8File (Join-Path $userdir 'var\log\messages.log') `
            'Product Version = Apache NetBeans IDE 30'

        { Assert-InstalledModuleFiles $userdir $metadata $catalogPath `
                $null -RequireActivation $false } | Should Not Throw
        try {
            Assert-InstalledModuleFiles $userdir $metadata $catalogPath `
                $null -RequireActivation $true
            $missingActivationRejected = $false
        } catch {
            $missingActivationRejected = $true
        }
        $missingActivationRejected | Should Be $true
    }

    It 'rejects stale installed bytes even when update tracking lists the file' {
        $nbm = New-NbmFixture (Join-Path $TestDrive 'stale\fixture.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalogPath = Join-Path $TestDrive 'stale\site\updates.xml'
        Write-Utf8File $catalogPath '<module_updates/>'
        $userdir = Join-Path $TestDrive 'stale\userdir'
        New-InstalledFixture $userdir $metadata $catalogPath
        Write-Utf8File (Join-Path $userdir `
            'modules\ext\dev.flutter.netbeans.netbeans-plugin\dependency.jar') `
            'stale previous-version bytes'

        $dependencyPath = Join-Path $userdir `
            'modules\ext\dev.flutter.netbeans.netbeans-plugin\dependency.jar'
        (Get-FileHash -LiteralPath $dependencyPath -Algorithm SHA256).Hash |
            Should Not Be $metadata.PayloadFiles[
                'modules/ext/dev.flutter.netbeans.netbeans-plugin/dependency.jar'].Sha256

        try {
            Assert-InstalledModuleFiles $userdir $metadata $catalogPath
            $staleRejected = $false
        } catch {
            $staleRejected = $true
        }
        $staleRejected | Should Be $true
    }
}
