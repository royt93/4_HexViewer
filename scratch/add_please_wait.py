import os

translations = {
    'values': {
        'please_wait': 'Please wait...'
    },
    'values-vi-rVN': {
        'please_wait': 'Vui lòng đợi...'
    },
    'values-ar': {
        'please_wait': 'يرجى الانتظار...'
    },
    'values-de-rDE': {
        'please_wait': 'Bitte warten...'
    },
    'values-es-rES': {
        'please_wait': 'Por favor, espere...'
    },
    'values-fr-rFR': {
        'please_wait': 'Veuillez patienter...'
    },
    'values-hi': {
        'please_wait': 'कृपया प्रतीक्षा करें...'
    },
    'values-hu-rHU': {
        'please_wait': 'Kérjük, várjon...'
    },
    'values-in': {
        'please_wait': 'Mohon tunggu...'
    },
    'values-it-rIT': {
        'please_wait': 'Attendere prego...'
    },
    'values-iw': {
        'please_wait': 'אנא המתן...'
    },
    'values-ja-rJP': {
        'please_wait': 'お待ちください...'
    },
    'values-nb-rNO': {
        'please_wait': 'Vennligst vent...'
    },
    'values-nl-rNL': {
        'please_wait': 'Even geduld a.u.b....'
    },
    'values-pt-rBR': {
        'please_wait': 'Por favor, aguarde...'
    },
    'values-pt-rPT': {
        'please_wait': 'Por favor, aguarde...'
    },
    'values-ru-rRU': {
        'please_wait': 'Пожалуйста, подождите...'
    },
    'values-tr-rTR': {
        'please_wait': 'Lütfen bekleyin...'
    },
    'values-zh-rCN': {
        'please_wait': '请稍候...'
    }
}

base_dir = "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/res"

for folder, strings_dict in translations.items():
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already updated to prevent duplication
        if 'please_wait' in content:
            print(f"Skipping {folder}, already has please_wait.")
            continue
            
        insertion = "\n    <!-- Extra please wait -->\n"
        for key, value in strings_dict.items():
            escaped_val = value.replace("'", "\\'").replace("?", "\\?")
            insertion += f'    <string name="{key}">{escaped_val}</string>\n'
            
        new_content = content.replace("</resources>", f"{insertion}</resources>")
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated extra strings in {folder}/strings.xml")
