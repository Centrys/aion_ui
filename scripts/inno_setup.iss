; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!
; Download Inno Setup from: http://www.jrsoftware.org/isdl.php to build this setup file

; !!!NOTICE FOR ANYONE TRYING TO BUILD THE SETUP!!!
;
;
; Anywhere below, when building the setup, make sure to replace "C:\Projects\aion_ui" with the actual path where you clone the `aion_ui`
;
; Before using the signtool, Windows 10 SDK should be installed - https://developer.microsoft.com/en-us/windows/downloads/windows-10-sdk
; A certificate needs to be added in Tools->Configure Sign Tools->Add
; Name=signtool
; Value="C:\Program Files (x86)\Windows Kits\10\App Certification Kit\signtool.exe"  sign /f "C:\Projects\aion_ui\scripts\cert.pfx" /p superaion /t http://timestamp.verisign.com/scripts/timstamp.dll $f
;
; If the current certificate has expired, a new one can be issued from powershell:
; > New-SelfSignedCertificate -certstorelocation cert:\localmachine\my -dnsname aion.network -type CodeSigning
; > certutil -exportPFX ${cert_hash_from_above_certificate} ${path_to_new_pfx_file}


#define MyAppName "AionWallet"
#define MyAppVersion "1.2.0"
#define MyAppPublisher "Aion"
#define MyAppURL "http://www.aion.network/"
#define MyAppExeName "AionWallet.exe"
#define MyAppUserDataDirName ".aion"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{49D782D3-43D8-47F2-914A-3DEA3D29CB62}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf64}\{#MyAppName}
DisableProgramGroupPage=yes
OutputBaseFilename=AionWalletSetup
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\pack\aion_ui\*"; DestDir: "{app}"; Excludes: "cert.pfx, unzip.exe, cygwin1.dll, cygbz2-1.dll, cygintl-8.dll, Bat_To_Exe.exe, *.zip, "; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: "..\pack\aion_ui\*.dll"; DestDir: "{tmp}"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{commonprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[InstallDelete]
; Remove old redundant files, to let us install directly without first uninstalling
; Remove any old existing 1.2.0 installs
Type: files; Name: "{app}/AionWallet.exe"
Type: filesandordirs; Name: "{app}/config"
Type: filesandordirs; Name: "{app}/java"
Type: filesandordirs; Name: "{app}/lib"
Type: filesandordirs; Name: "{app}/mod"
Type: filesandordirs; Name: "{app}/native"
; Remove any old existing 1.1.0 installs
Type: filesandordirs; Name: "{app}/jre-10.0.2"

[UninstallRun]
Filename: "PowerShell.exe"; Parameters: "-windowstyle hidden -Command ""& {{robocopy /MIR '{app}\lib' '{app}\native'}""";

[UninstallDelete]
Type: filesandordirs; Name:"{app}";
