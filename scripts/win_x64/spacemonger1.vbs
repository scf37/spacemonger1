Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

' Get the directory where this script resides
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)

' Build the path to java.exe
javaExe = fso.BuildPath(scriptDir, "bin\java.exe")

' Command to run
cmd = """" & javaExe & """ -XX:+UseSerialGC --enable-native-access=spacemonger -m spacemonger/spacemonger1.App"

' Run it hidden (0 = hidden window, True = wait until exit)
shell.Run cmd, 0, False