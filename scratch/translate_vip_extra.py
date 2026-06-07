import os

extra_translations = {
    'values': {
        'vip_premium_packages': 'Premium Packages',
        'vip_revoked_message': 'VIP Revoked',
        'vip_success_message': 'Successfully activated VIP for %1$d days!',
        'vip_failed_message': 'Invalid VIP Key. Please try again.',
        'vip_open_link_error': 'Unable to open link',
        'vip_badge_text': 'VIP'
    },
    'values-vi-rVN': {
        'vip_premium_packages': 'Gói Premium',
        'vip_revoked_message': 'Đã thu hồi VIP',
        'vip_success_message': 'Kích hoạt VIP thành công %1$d ngày!',
        'vip_failed_message': 'Mã VIP không hợp lệ. Vui lòng thử lại.',
        'vip_open_link_error': 'Không thể mở liên kết',
        'vip_badge_text': 'VIP'
    },
    'values-ar': {
        'vip_premium_packages': 'باقات بريميوم',
        'vip_revoked_message': 'تم إلغاء عضوية VIP',
        'vip_success_message': 'تم تفعيل VIP بنجاح لمدة %1$d يومًا!',
        'vip_failed_message': 'رمز VIP غير صالح. يرجى المحاولة مرة أخرى.',
        'vip_open_link_error': 'تعذر فتح الرابط',
        'vip_badge_text': 'VIP'
    },
    'values-de-rDE': {
        'vip_premium_packages': 'Premium-Pakete',
        'vip_revoked_message': 'VIP entzogen',
        'vip_success_message': 'VIP erfolgreich für %1$d Tage aktiviert!',
        'vip_failed_message': 'Ungültiger VIP-Schlüssel. Bitte versuchen Sie es erneut.',
        'vip_open_link_error': 'Link kann nicht geöffnet werden',
        'vip_badge_text': 'VIP'
    },
    'values-es-rES': {
        'vip_premium_packages': 'Paquetes Premium',
        'vip_revoked_message': 'VIP revocado',
        'vip_success_message': '¡VIP activado con éxito por %1$d días!',
        'vip_failed_message': 'Clave VIP no válida. Por favor, inténtelo de nuevo.',
        'vip_open_link_error': 'No se pudo abrir el enlace',
        'vip_badge_text': 'VIP'
    },
    'values-fr-rFR': {
        'vip_premium_packages': 'Forfaits Premium',
        'vip_revoked_message': 'VIP révoqué',
        'vip_success_message': 'VIP activé avec succès pour %1$d jours !',
        'vip_failed_message': 'Clé VIP invalide. Veuillez réessayer.',
        'vip_open_link_error': 'Impossible d\'ouvrir le lien',
        'vip_badge_text': 'VIP'
    },
    'values-hi': {
        'vip_premium_packages': 'प्रीमियम पैकेज',
        'vip_revoked_message': 'वीआईपी वापस ले लिया गया',
        'vip_success_message': 'सफलतापूर्वक %1$d दिनों के लिए वीआईपी सक्रिय किया गया!',
        'vip_failed_message': 'अमान्य वीआईपी कुंजी। कृपया पुनः प्रयास करें।',
        'vip_open_link_error': 'लिंक खोलने में असमर्थ',
        'vip_badge_text': 'VIP'
    },
    'values-hu-rHU': {
        'vip_premium_packages': 'Prémium csomagok',
        'vip_revoked_message': 'VIP visszavonva',
        'vip_success_message': 'A VIP státusz sikeresen aktiválva %1$d napra!',
        'vip_failed_message': 'Érvénytelen VIP kód. Kérjük, próbálja újra.',
        'vip_open_link_error': 'A link megnyitása sikertelen',
        'vip_badge_text': 'VIP'
    },
    'values-in': {
        'vip_premium_packages': 'Paket Premium',
        'vip_revoked_message': 'VIP dicabut',
        'vip_success_message': 'VIP berhasil diaktifkan selama %1$d hari!',
        'vip_failed_message': 'Kunci VIP tidak valid. Silakan coba lagi.',
        'vip_open_link_error': 'Gagal membuka tautan',
        'vip_badge_text': 'VIP'
    },
    'values-it-rIT': {
        'vip_premium_packages': 'Pacchetti Premium',
        'vip_revoked_message': 'VIP revocato',
        'vip_success_message': 'VIP attivato con successo per %1$d giorni!',
        'vip_failed_message': 'Codice VIP non valido. Riprova.',
        'vip_open_link_error': 'Impossibile aprire il link',
        'vip_badge_text': 'VIP'
    },
    'values-iw': {
        'vip_premium_packages': 'חבילות פרימיום',
        'vip_revoked_message': 'חברות VIP בוטלה',
        'vip_success_message': 'חברות VIP הופעלה בהצלحة למשך %1$d ימים!',
        'vip_failed_message': 'קוד VIP לא תקין. אנא נסה שנית.',
        'vip_open_link_error': 'לא ניתן לפתוح את הקישור',
        'vip_badge_text': 'VIP'
    },
    'values-ja-rJP': {
        'vip_premium_packages': 'プレミアムパッケージ',
        'vip_revoked_message': 'VIP権限が取り消されました',
        'vip_success_message': 'VIPが %1$d 日間正常に有効化されました！',
        'vip_failed_message': '無効なVIPキーです。もう一度お試しください。',
        'vip_open_link_error': 'リンクを開くことができません',
        'vip_badge_text': 'VIP'
    },
    'values-nb-rNO': {
        'vip_premium_packages': 'Premium-pakker',
        'vip_revoked_message': 'VIP fjernet',
        'vip_success_message': 'VIP er aktivert for %1$d dager!',
        'vip_failed_message': 'Ugyldig VIP-nøkkel. Vennligst prøv igjen.',
        'vip_open_link_error': 'Kunne ikke åpne lenken',
        'vip_badge_text': 'VIP'
    },
    'values-nl-rNL': {
        'vip_premium_packages': 'Premium-pakketten',
        'vip_revoked_message': 'VIP ingetrokken',
        'vip_success_message': 'VIP succesvol geactiveerd voor %1$d dagen!',
        'vip_failed_message': 'Ongeldige VIP-sleutel. Probeer het opnieuw.',
        'vip_open_link_error': 'Kan link niet openen',
        'vip_badge_text': 'VIP'
    },
    'values-pt-rBR': {
        'vip_premium_packages': 'Pacotes Premium',
        'vip_revoked_message': 'VIP revogado',
        'vip_success_message': 'VIP ativado com sucesso por %1$d dias!',
        'vip_failed_message': 'Chave VIP inválida. Por favor, tente novamente.',
        'vip_open_link_error': 'Não foi possível abrir o link',
        'vip_badge_text': 'VIP'
    },
    'values-pt-rPT': {
        'vip_premium_packages': 'Pacotes Premium',
        'vip_revoked_message': 'VIP revogado',
        'vip_success_message': 'VIP ativado com sucesso por %1$d dias!',
        'vip_failed_message': 'Chave VIP inválida. Por favor, tente novamente.',
        'vip_open_link_error': 'Não foi possível abrir a ligação',
        'vip_badge_text': 'VIP'
    },
    'values-ru-rRU': {
        'vip_premium_packages': 'Премиум-пакеты',
        'vip_revoked_message': 'VIP аннулирован',
        'vip_success_message': 'VIP успешно активирован на %1$d дн.!',
        'vip_failed_message': 'Неверный VIP-ключ. Пожалуйста, попробуйте снова.',
        'vip_open_link_error': 'Не удалось открыть ссылку',
        'vip_badge_text': 'VIP'
    },
    'values-tr-rTR': {
        'vip_premium_packages': 'Premium Paketler',
        'vip_revoked_message': 'VIP iptal edildi',
        'vip_success_message': 'VIP %1$d günlük başarıyla etkinleştirildi!',
        'vip_failed_message': 'Geçersiz VIP Kodu. Lütfen tekrar deneyin.',
        'vip_open_link_error': 'Bağlantı açılamıyor',
        'vip_badge_text': 'VIP'
    },
    'values-zh-rCN': {
        'vip_premium_packages': '优质套餐',
        'vip_revoked_message': 'VIP已撤销',
        'vip_success_message': '成功激活 VIP %1$d 天！',
        'vip_failed_message': 'VIP 激活码无效。请重试。',
        'vip_open_link_error': '无法打开链接',
        'vip_badge_text': 'VIP'
    }
}

base_dir = "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/res"

for folder, strings_dict in extra_translations.items():
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already updated to prevent duplication
        if 'vip_premium_packages' in content:
            print(f"Skipping {folder}, already has extra VIP strings.")
            continue
            
        insertion = "\n    <!-- Extra VIP Strings -->\n"
        for key, value in strings_dict.items():
            # Escape single quotes and question marks
            escaped_val = value.replace("'", "\\'").replace("?", "\\?")
            insertion += f'    <string name="{key}">{escaped_val}</string>\n'
            
        new_content = content.replace("</resources>", f"{insertion}</resources>")
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated extra strings in {folder}/strings.xml")
