import os
import glob

base_dir = "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260207HexViewer/app/src/main/res"

for filepath in glob.glob(f"{base_dir}/values-*/strings.xml"):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    modified = False
    for i, line in enumerate(lines):
        if "name=\"file_info_" in line:
            if "'" in line and "\\'" not in line:
                lines[i] = line.replace("'", "\\'")
                modified = True
                
    if modified:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(lines)
        print(f"Fixed {filepath}")

