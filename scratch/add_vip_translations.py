import os

translations = {
    'values': {
        'vip_member_title': 'VIP Member',
        'vip_badge_get_vip': 'GET VIP'
    },
    'values-vi-rVN': {
        'vip_member_title': 'Thành viên VIP',
        'vip_badge_get_vip': 'NHẬN VIP'
    },
    'values-ar': {
        'vip_member_title': 'عضوية VIP',
        'vip_badge_get_vip': 'الحصول على VIP'
    },
    'values-de-rDE': {
        'vip_member_title': 'VIP-Mitglied',
        'vip_badge_get_vip': 'VIP HOLEN'
    },
    'values-es-rES': {
        'vip_member_title': 'Miembro VIP',
        'vip_badge_get_vip': 'OBTENER VIP'
    },
    'values-fr-rFR': {
        'vip_member_title': 'Membre VIP',
        'vip_badge_get_vip': 'OBTENIR VIP'
    },
    'values-hi': {
        'vip_member_title': 'वीआईपी सदस्य',
        'vip_badge_get_vip': 'वीआईपी प्राप्त करें'
    },
    'values-hu-rHU': {
        'vip_member_title': 'VIP Tag',
        'vip_badge_get_vip': 'VIP IGÉNYLÉSE'
    },
    'values-in': {
        'vip_member_title': 'Anggota VIP',
        'vip_badge_get_vip': 'DAPATKAN VIP'
    },
    'values-it-rIT': {
        'vip_member_title': 'Membro VIP',
        'vip_badge_get_vip': 'OTTIENI VIP'
    },
    'values-iw': {
        'vip_member_title': 'חבר VIP',
        'vip_badge_get_vip': 'קבל VIP'
    },
    'values-ja-rJP': {
        'vip_member_title': 'VIPメンバー',
        'vip_badge_get_vip': 'VIPを取得'
    },
    'values-nb-rNO': {
        'vip_member_title': 'VIP-medlem',
        'vip_badge_get_vip': 'FÅ VIP'
    },
    'values-nl-rNL': {
        'vip_member_title': 'VIP-lid',
        'vip_badge_get_vip': 'VIP KRIJGEN'
    },
    'values-pt-rBR': {
        'vip_member_title': 'Membro VIP',
        'vip_badge_get_vip': 'OBTER VIP'
    },
    'values-pt-rPT': {
        'vip_member_title': 'Membro VIP',
        'vip_badge_get_vip': 'OBTER VIP'
    },
    'values-ru-rRU': {
        'vip_member_title': 'VIP-участник',
        'vip_badge_get_vip': 'ПОЛУЧИТЬ VIP'
    },
    'values-tr-rTR': {
        'vip_member_title': 'VIP Üye',
        'vip_badge_get_vip': 'VIP EDİN'
    },
    'values-zh-rCN': {
        'vip_member_title': 'VIP 会员',
        'vip_badge_get_vip': '获取 VIP'
    }
}

base_dir = "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/res"

for folder, strings_dict in translations.items():
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already updated to prevent duplication
        if 'vip_member_title' in content:
            print(f"Skipping {folder}, already has vip_member_title.")
            continue
            
        insertion = "\n    <!-- Extra VIP Title & Badge -->\n"
        for key, value in strings_dict.items():
            escaped_val = value.replace("'", "\\'").replace("?", "\\?")
            insertion += f'    <string name="{key}">{escaped_val}</string>\n'
            
        new_content = content.replace("</resources>", f"{insertion}</resources>")
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated extra strings in {folder}/strings.xml")
