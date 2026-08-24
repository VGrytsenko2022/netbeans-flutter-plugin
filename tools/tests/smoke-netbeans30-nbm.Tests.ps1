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
    New-ZipFile $Path @{
        'Info/info.xml' = $info
        'netbeans/config/Modules/dev-flutter-netbeans-netbeans-plugin.xml' = $config
        'netbeans/modules/dev-flutter-netbeans-netbeans-plugin.jar' = `
            [System.IO.File]::ReadAllBytes($moduleJar)
        'netbeans/modules/ext/dev.flutter.netbeans.netbeans-plugin/dependency.jar' = `
            [System.Text.Encoding]::UTF8.GetBytes("dependency-$ImplementationVersion")
    }
    return $Path
}

function New-InstalledFixture {
    param(
        [string]$Userdir,
        [pscustomobject]$Metadata,
        [string]$CatalogPath
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

Describe 'smoke-netbeans30-nbm.ps1 pure helpers' {
    It 'stages an immutable local catalog from the exact NBM bytes' {
        $nbm = New-NbmFixture (Join-Path $TestDrive `
            'artifact\netbeans-plugin-0.1.2-SNAPSHOT.nbm')
        $metadata = Get-NbmMetadata $nbm
        $catalog = New-LocalUpdateCatalog $metadata (Join-Path $TestDrive 'site')

        $metadata.CodeName | Should Be 'dev.flutter.netbeans.netbeans.plugin'
        $metadata.SpecificationVersion | Should Be '0.1.2'
        $metadata.PayloadFiles.Count | Should Be 3
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

        [void](Wait-NetBeansCliReady $hostHandle 'netbeans64.exe' `
                (Join-Path $TestDrive 'retry\userdir') 10 10)

        $script:cliAttempts | Should Be 2
        $cacheIndex = [Array]::IndexOf($script:observedArguments, '--cachedir')
        $cacheIndex | Should BeGreaterThan -1
        $script:observedArguments[$cacheIndex + 1] | Should Be $cachedir
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
