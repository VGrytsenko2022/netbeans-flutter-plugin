$Verifier = (Resolve-Path (Join-Path $PSScriptRoot '..\verify-release.ps1')).Path
$PowerShellExecutable = (Get-Process -Id $PID).Path

function Write-Utf8File {
    param(
        [string]$Path,
        [string]$Content
    )
    $parent = Split-Path -Parent $Path
    [void](New-Item -ItemType Directory -Path $parent -Force)
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

function New-ZipFile {
    param(
        [string]$Path,
        [hashtable]$Entries
    )
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $parent = Split-Path -Parent $Path
    [void](New-Item -ItemType Directory -Path $parent -Force)
    $archive = [System.IO.Compression.ZipFile]::Open(
        $Path, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($name in $Entries.Keys) {
            $entry = $archive.CreateEntry($name)
            $stream = $entry.Open()
            $writer = New-Object System.IO.StreamWriter -ArgumentList $stream
            try {
                $writer.Write([string]$Entries[$name])
            } finally {
                $writer.Dispose()
                $stream.Dispose()
            }
        }
    } finally {
        $archive.Dispose()
    }
}

function New-ReleaseFixture {
    param(
        [string]$Root,
        [string]$Category = 'Flutter',
        [switch]$OptionalSdkSkip,
        [switch]$InstalledUserdir,
        [switch]$CriticalLog,
        [switch]$AuxiliaryOrderingLog,
        [switch]$PluginLayerOrderingLog,
        [switch]$RotatedPluginLayerOrderingLog,
        [switch]$UnrelatedOrderingLog,
        [switch]$CatalogUpdaterOrigin,
        [switch]$OrphanUpdaterOrigin
    )

    $version = '0.1.2'
    $license = "Apache License`nVersion 2.0"
    Write-Utf8File (Join-Path $Root 'pom.xml') @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId><artifactId>fixture</artifactId><version>$version</version>
  <modules><module>netbeans-plugin</module></modules>
</project>
"@
    Write-Utf8File (Join-Path $Root 'LICENSE') $license
    Write-Utf8File (Join-Path $Root 'netbeans-plugin\pom.xml') @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>test</groupId><artifactId>fixture</artifactId><version>$version</version></parent>
  <artifactId>netbeans-plugin</artifactId>
</project>
"@
    Write-Utf8File (Join-Path $Root `
        'netbeans-plugin\src\test\java\example\SampleTest.java') `
        'package example; class SampleTest {}'
    Write-Utf8File (Join-Path $Root `
        'netbeans-plugin\src\test\java\example\PluginPackageMetadataIT.java') `
        'package example; class PluginPackageMetadataIT {}'
    Write-Utf8File (Join-Path $Root `
        'netbeans-plugin\target\surefire-reports\TEST-example.SampleTest.xml') @"
<testsuite name="example.SampleTest" tests="1" failures="0" errors="0" skipped="0">
  <testcase classname="example.SampleTest" name="passes"/>
</testsuite>
"@
    Write-Utf8File (Join-Path $Root `
        'netbeans-plugin\target\failsafe-reports\TEST-example.PluginPackageMetadataIT.xml') @"
<testsuite name="example.PluginPackageMetadataIT" tests="1" failures="0" errors="0" skipped="0">
  <testcase classname="example.PluginPackageMetadataIT" name="passes"/>
</testsuite>
"@
    if ($OptionalSdkSkip) {
        Write-Utf8File (Join-Path $Root `
            'netbeans-plugin\src\test\java\dev\flutter\netbeans\dart\DartAnalysisServerRealSdkTest.java') `
            'package dev.flutter.netbeans.dart; class DartAnalysisServerRealSdkTest {}'
        Write-Utf8File (Join-Path $Root `
            'netbeans-plugin\target\surefire-reports\TEST-dev.flutter.netbeans.dart.DartAnalysisServerRealSdkTest.xml') @"
<testsuite name="dev.flutter.netbeans.dart.DartAnalysisServerRealSdkTest"
           tests="1" failures="0" errors="0" skipped="1">
  <testcase classname="dev.flutter.netbeans.dart.DartAnalysisServerRealSdkTest" name="smoke">
    <skipped message="Dart SDK was not configured"/>
  </testcase>
</testsuite>
"@
        Write-Utf8File (Join-Path $Root `
            'netbeans-plugin\src\test\java\dev\flutter\netbeans\runtime\DartEditorEndToEndIT.java') `
            'package dev.flutter.netbeans.runtime; class DartEditorEndToEndIT {}'
        Write-Utf8File (Join-Path $Root `
            'netbeans-plugin\target\failsafe-reports\TEST-dev.flutter.netbeans.runtime.DartEditorEndToEndIT.xml') @"
<testsuite name="dev.flutter.netbeans.runtime.DartEditorEndToEndIT"
           tests="1" failures="0" errors="0" skipped="1">
  <testcase classname="dev.flutter.netbeans.runtime.DartEditorEndToEndIT" name="editorSmoke">
    <skipped message="Dart SDK was not configured"/>
  </testcase>
</testsuite>
"@
    }

    $nbmPath = Join-Path $Root 'netbeans-plugin\target\netbeans-plugin-0.1.2.nbm'
    $info = @"
<?xml version="1.0" encoding="UTF-8"?>
<module codenamebase="dev.flutter.netbeans.netbeans.plugin"
        distribution="netbeans-plugin-0.1.2.nbm" license="license-id">
  <manifest OpenIDE-Module="dev.flutter.netbeans.netbeans.plugin"
            OpenIDE-Module-Name="Flutter and Dart Support"
            OpenIDE-Module-Display-Category="$Category"
            OpenIDE-Module-Specification-Version="0.1.2"
            OpenIDE-Module-Implementation-Version="0.1.2"/>
  <license name="license-id">$license</license>
</module>
"@
    New-ZipFile $nbmPath @{
        'Info/info.xml' = $info
        'netbeans/config/Modules/dev-flutter-netbeans-netbeans-plugin.xml' = '<module/>'
        'netbeans/modules/dev-flutter-netbeans-netbeans-plugin.jar' = 'fixture'
    }

    $userdir = $null
    if ($InstalledUserdir) {
        $userdir = Join-Path $Root 'installed-userdir'
        $config = @"
<module name="dev.flutter.netbeans.netbeans.plugin">
  <param name="enabled">true</param>
  <param name="jar">modules/dev-flutter-netbeans-netbeans-plugin.jar</param>
</module>
"@
        Write-Utf8File (Join-Path $userdir `
            'config\Modules\dev-flutter-netbeans-netbeans-plugin.xml') $config
        if ($CatalogUpdaterOrigin -or $OrphanUpdaterOrigin) {
            $site = Join-Path $Root 'installed-site'
            [void](New-Item -ItemType Directory -Path $site -Force)
            $stagedNbm = Join-Path $site (Split-Path -Leaf $nbmPath)
            Copy-Item -LiteralPath $nbmPath -Destination $stagedNbm
            $sha512 = (Get-FileHash -LiteralPath $nbmPath -Algorithm SHA512).Hash.ToLowerInvariant()
            $length = (Get-Item -LiteralPath $nbmPath).Length
            $catalogPath = Join-Path $site 'updates.xml'
            Write-Utf8File $catalogPath @"
<module_updates>
  <module codenamebase="dev.flutter.netbeans.netbeans.plugin"
          distribution="netbeans-plugin-0.1.2.nbm" downloadsize="$length">
    <manifest OpenIDE-Module="dev.flutter.netbeans.netbeans.plugin"
              OpenIDE-Module-Specification-Version="0.1.2"/>
    <message_digest algorithm="SHA-512" value="$sha512"/>
  </module>
</module_updates>
"@
            $catalogOrigin = [System.Uri]::new($catalogPath).AbsoluteUri
            $catalogVersion = if ($OrphanUpdaterOrigin) {
                ''
            } else {
                @"
  <module_version last="false" origin="$catalogOrigin"
                  specification_version="0.1.2">
    <file name="config/Modules/dev-flutter-netbeans-netbeans-plugin.xml"/>
    <file name="modules/dev-flutter-netbeans-netbeans-plugin.jar"/>
  </module_version>
"@
            }
            $tracking = @"
<module codename="dev.flutter.netbeans.netbeans.plugin">
$catalogVersion
  <module_version last="true" origin="updater"
                  specification_version="0.1.2">
    <file name="config/Modules/dev-flutter-netbeans-netbeans-plugin.xml"/>
    <file name="modules/dev-flutter-netbeans-netbeans-plugin.jar"/>
  </module_version>
</module>
"@
        } else {
            $tracking = @"
<module codename="dev.flutter.netbeans.netbeans.plugin">
  <module_version last="true" origin="netbeans-plugin-0.1.2.nbm"
                  specification_version="0.1.2">
    <file name="config/Modules/dev-flutter-netbeans-netbeans-plugin.xml"/>
    <file name="modules/dev-flutter-netbeans-netbeans-plugin.jar"/>
  </module_version>
</module>
"@
        }
        Write-Utf8File (Join-Path $userdir `
            'update_tracking\dev-flutter-netbeans-netbeans-plugin.xml') $tracking
        New-ZipFile (Join-Path $userdir 'modules\dev-flutter-netbeans-netbeans-plugin.jar') @{
            'META-INF/MANIFEST.MF' = @"
Manifest-Version: 1.0
OpenIDE-Module: dev.flutter.netbeans.netbeans.plugin
OpenIDE-Module-Specification-Version: 0.1.2
OpenIDE-Module-Implementation-Version: 0.1.2

"@
        }
        $critical = if ($CriticalLog) { "`nSEVERE synthetic release smoke failure" } else { '' }
        $auxiliaryOrdering = if ($AuxiliaryOrderingLog) {
            $orderingWarnings = @'
WARNING [org.openide.filesystems.Ordering]: Encountered non-boolean relative ordering attribute <open-files xmlns="http://www.netbeans.org/ns/projectui-open-files/2"><group/></open-files> from org.netbeans.spi.project.AuxiliaryConfiguration.http://www.netbeans.org/ns/projectui-open-files/2#open-files on C:/fixture/flutter_app
WARNING [org.openide.filesystems.Ordering]: Encountered non-boolean relative ordering attribute <preferences xmlns="http://www.netbeans.org/ns/auxiliary-configuration-preferences/1"/> from org.netbeans.spi.project.AuxiliaryConfiguration.http://www.netbeans.org/ns/auxiliary-configuration-preferences/1#preferences on C:/fixture/flutter_app
WARNING [org.openide.filesystems.Ordering]: Encountered non-boolean relative ordering attribute <editor-bookmarks xmlns="http://www.netbeans.org/ns/editor-bookmarks/2"/> from org.netbeans.spi.project.AuxiliaryConfiguration.http://www.netbeans.org/ns/editor-bookmarks/2#editor-bookmarks on C:/fixture/flutter_app
'@
            "`n$orderingWarnings"
        } else {
            ''
        }
        $pluginLayerOrdering = if ($PluginLayerOrderingLog) {
            "`nWARNING [org.openide.filesystems.Ordering]: Found same position 100 for both dev-flutter-netbeans-plugin-dart-DartTokenId.instance and dev-flutter-netbeans-plugin-dart-DartEditorKit.instance"
        } else {
            ''
        }
        $unrelatedOrdering = if ($UnrelatedOrderingLog) {
            $unrelatedWarnings = @'
WARNING [org.openide.filesystems.Ordering]: Found same position 100 for both org-example-One.instance and org-example-Two.instance
WARNING [org.openide.filesystems.Ordering]: Not all children in Editors/text/x-dart/ marked with the position attribute: [org-example-One.instance]
WARNING [org.openide.filesystems.Ordering]: Could not find both sides of relative ordering attribute dev-flutter-netbeans-plugin-dart-DartTokenId.instance/org-example-One.instance
INFO [org.openide.filesystems.Ordering]: Found same position 100 for both dev-flutter-netbeans-plugin-dart-DartTokenId.instance and org-example-One.instance
WARNING [org.openide.filesystems.Other]: Not all children in Editors/text/x-dart/ marked with the position attribute: [dev-flutter-netbeans-plugin-dart-DartTokenId.instance]
'@
            "`n$unrelatedWarnings"
        } else {
            ''
        }
        Write-Utf8File (Join-Path $userdir 'var\log\messages.log') @"
  Product Version         = Apache NetBeans IDE 30
INFO [org.netbeans.core.startup.NbEvents]: Turning on modules:
    dev.flutter.netbeans.netbeans.plugin [0.1.2 0.1.2 202608250001]$critical$auxiliaryOrdering$pluginLayerOrdering$unrelatedOrdering
"@
        if ($RotatedPluginLayerOrderingLog) {
            Write-Utf8File (Join-Path $userdir 'var\log\messages.log.1') @'
WARNING [org.openide.filesystems.Ordering]: Not all children in Editors/text/x-yaml/CodeTemplates/ marked with the position attribute: [dev-flutter-netbeans-plugin-pubspec-PubspecErrorProvider.instance], but some are: [org-netbeans-modules-editor-codegen-main.instance]
'@
        }
    }

    return [pscustomobject]@{
        Root = $Root
        Nbm = $nbmPath
        Userdir = $userdir
    }
}

function Invoke-ReleaseVerifier {
    param(
        [pscustomobject]$Fixture,
        [switch]$InstalledUserdir,
        [switch]$RequireOptionalSdkTests
    )
    $arguments = @(
        '-NoProfile',
        '-File', $Verifier,
        '-RepositoryRoot', $Fixture.Root,
        '-Version', '0.1.2',
        '-NbmPath', $Fixture.Nbm,
        '-SkipFreshnessCheck'
    )
    if ($InstalledUserdir) {
        $arguments += @('-InstalledUserdir', $Fixture.Userdir)
    }
    if ($RequireOptionalSdkTests) {
        $arguments += '-RequireOptionalSdkTests'
    }
    $output = @(& $PowerShellExecutable @arguments 2>&1)
    return [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Text = $output -join "`n"
    }
}

Describe 'verify-release.ps1' {
    It 'accepts a valid NBM and complete passing Maven reports' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'valid')

        $result = Invoke-ReleaseVerifier $fixture

        $result.ExitCode | Should Be 0
        $result.Text | Should Match 'Release verification PASSED'
        $result.Text | Should Match 'Surefire: tests=1'
        $result.Text | Should Match 'Failsafe: tests=1'
        $result.Text | Should Match 'SHA256 [0-9A-F]{64}'
    }

    It 'rejects incorrect NBM Plugin Manager metadata' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'bad-metadata') `
            -Category 'Not Flutter'

        $result = Invoke-ReleaseVerifier $fixture

        $result.ExitCode | Should Be 1
        $result.Text | Should Match "Module category is 'Not Flutter'; expected 'Flutter'"
        $result.Text | Should Match 'Release verification FAILED'
    }

    It 'rejects critical errors from an installed NetBeans userdir log' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'critical-log') `
            -InstalledUserdir -CriticalLog

        $result = Invoke-ReleaseVerifier $fixture -InstalledUserdir

        $result.ExitCode | Should Be 1
        $result.Text | Should Match 'messages.log contains critical pattern'
        $result.Text | Should Match 'SEVERE synthetic release smoke failure'
    }

    It 'rejects AuxiliaryConfiguration ordering warnings from an installed userdir log' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'auxiliary-ordering-log') `
            -InstalledUserdir -AuxiliaryOrderingLog

        $result = Invoke-ReleaseVerifier $fixture -InstalledUserdir

        $result.ExitCode | Should Be 1
        $result.Text | Should Match 'NetBeans logs contain plugin-owned Ordering warning'
        $result.Text | Should Match 'projectui-open-files/2#open-files'
        $result.Text | Should Match 'auxiliary-configuration-preferences/1#preferences'
        $result.Text | Should Match 'editor-bookmarks/2#editor-bookmarks'
        $result.Text | Should Match 'Release verification FAILED'
    }

    It 'rejects plugin layer Ordering warnings from current and retained userdir logs' {
        $currentFixture = New-ReleaseFixture (Join-Path $TestDrive `
            'plugin-ordering-current') -InstalledUserdir -PluginLayerOrderingLog
        $currentResult = Invoke-ReleaseVerifier $currentFixture -InstalledUserdir

        $currentResult.ExitCode | Should Be 1
        $currentResult.Text | Should Match 'NetBeans logs contain plugin-owned Ordering warning'
        $currentResult.Text | Should Match 'messages.log:.*Found same position 100'

        $rotatedFixture = New-ReleaseFixture (Join-Path $TestDrive `
            'plugin-ordering-rotated') -InstalledUserdir -RotatedPluginLayerOrderingLog
        $rotatedResult = Invoke-ReleaseVerifier $rotatedFixture -InstalledUserdir

        $rotatedResult.ExitCode | Should Be 1
        $rotatedResult.Text | Should Match 'NetBeans logs contain plugin-owned Ordering warning'
        $rotatedResult.Text | Should Match 'messages.log.1:.*Not all children'
    }

    It 'ignores unrelated Ordering warnings and plugin lines outside the two templates' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive `
            'unrelated-ordering-log') -InstalledUserdir -UnrelatedOrderingLog

        $result = Invoke-ReleaseVerifier $fixture -InstalledUserdir

        $result.ExitCode | Should Be 0
        $result.Text | Should Match 'NetBeans logs contain no plugin-owned Ordering warnings'
        $result.Text | Should Match 'Release verification PASSED'
    }

    It 'accepts a CLI updater origin backed by an exact local catalog' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'catalog-updater') `
            -InstalledUserdir -CatalogUpdaterOrigin

        $result = Invoke-ReleaseVerifier $fixture -InstalledUserdir

        $result.ExitCode | Should Be 0
        $result.Text | Should Match "Tracked updater origin is backed by verified local catalog"
        $result.Text | Should Match 'Release verification PASSED'
    }

    It 'rejects an updater origin without a same-version artifact origin' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'orphan-updater') `
            -InstalledUserdir -OrphanUpdaterOrigin

        $result = Invoke-ReleaseVerifier $fixture -InstalledUserdir

        $result.ExitCode | Should Be 1
        $result.Text | Should Match "Tracked origin is 'updater'"
        $result.Text | Should Match 'no same-version entry'
        $result.Text | Should Match 'Release verification FAILED'
    }

    It 'rejects a tracked catalog whose digest does not match the NBM' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'bad-catalog-digest') `
            -InstalledUserdir -CatalogUpdaterOrigin
        $catalogPath = Join-Path $fixture.Root 'installed-site\updates.xml'
        $catalog = [System.IO.File]::ReadAllText($catalogPath)
        $catalog = $catalog -replace 'value="[0-9a-f]{128}"', `
            ('value="' + ('0' * 128) + '"')
        Write-Utf8File $catalogPath $catalog

        $result = Invoke-ReleaseVerifier $fixture -InstalledUserdir

        $result.ExitCode | Should Be 1
        $result.Text | Should Match 'catalog SHA-512 does not match the verified NBM'
        $result.Text | Should Match 'Release verification FAILED'
    }

    It 'allows only known optional SDK skips unless they are required' {
        $fixture = New-ReleaseFixture (Join-Path $TestDrive 'optional-sdk') `
            -OptionalSdkSkip

        $allowed = Invoke-ReleaseVerifier $fixture
        $required = Invoke-ReleaseVerifier $fixture -RequireOptionalSdkTests

        $allowed.ExitCode | Should Be 0
        $allowed.Text | Should Match 'Allowed optional SDK skips'
        $allowed.Text | Should Match 'dev\.flutter\.netbeans\.runtime\.DartEditorEndToEndIT'
        $required.ExitCode | Should Be 1
        $required.Text | Should Match 'RequireOptionalSdkTests was set'
    }
}
