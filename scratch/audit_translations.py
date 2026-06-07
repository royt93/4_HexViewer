import os
import xml.etree.ElementTree as ET

res_dir = "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/res"
default_file = os.path.join(res_dir, "values", "strings.xml")

def parse_keys(file_path):
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        return set(elem.attrib['name'] for elem in root.findall('string'))
    except Exception as e:
        print(f"Error parsing {file_path}: {e}")
        return set()

default_keys = parse_keys(default_file)
vip_keys = {k for k in default_keys if k.startswith('vip_') or k in ['privacy_policy', 'confirm', 'ok', 'cancel']}

print(f"Total default VIP keys: {len(vip_keys)}")

folders = [f for f in os.listdir(res_dir) if f.startswith('values-') and f not in ['values-night', 'values-ldrtl', 'values-v31', 'values-v35']]

for folder in folders:
    file_path = os.path.join(res_dir, folder, "strings.xml")
    if not os.path.exists(file_path):
        print(f"File missing: {folder}/strings.xml")
        continue
    keys = parse_keys(file_path)
    missing = vip_keys - keys
    if missing:
        print(f"Folder {folder} is missing VIP keys: {missing}")
    else:
        print(f"Folder {folder} is fully translated.")
