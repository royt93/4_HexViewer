import os
import xml.etree.ElementTree as ET

translations = {
    'values-ar': {'file_info_title': 'محلل معلومات الملف', 'file_info_select_file_hint': 'انقر لتحديد ملف للتحليل', 'file_info_magic_bytes': 'بايتات سحرية (الرأس)', 'file_info_detected_type': 'نوع الملف المكتشف', 'file_info_unknown': 'بيانات ثنائية غير معروفة', 'file_info_analyzing': 'جاري تحليل رأس الملف...'},
    'values-de-rDE': {'file_info_title': 'Datei-Info-Analysator', 'file_info_select_file_hint': 'Tippen Sie, um eine Datei auszuwählen', 'file_info_magic_bytes': 'Magische Bytes (Header)', 'file_info_detected_type': 'Erkannter Dateityp', 'file_info_unknown': 'Unbekannte Binärdaten', 'file_info_analyzing': 'Dateikopf wird analysiert...'},
    'values-es-rES': {'file_info_title': 'Analizador de info de archivo', 'file_info_select_file_hint': 'Toque para seleccionar un archivo', 'file_info_magic_bytes': 'Bytes mágicos (Cabecera)', 'file_info_detected_type': 'Tipo de archivo detectado', 'file_info_unknown': 'Datos binarios desconocidos', 'file_info_analyzing': 'Analizando cabecera del archivo...'},
    'values-fr-rFR': {'file_info_title': "Analyseur d'infos de fichier", 'file_info_select_file_hint': 'Appuyez pour sélectionner un fichier', 'file_info_magic_bytes': 'Octets magiques (En-tête)', 'file_info_detected_type': 'Type de fichier détecté', 'file_info_unknown': 'Données binaires inconnues', 'file_info_analyzing': "Analyse de l'en-tête..."},
    'values-hi': {'file_info_title': 'फ़ाइल जानकारी विश्लेषक', 'file_info_select_file_hint': 'एक फ़ाइल चुनें', 'file_info_magic_bytes': 'जादुई बाइट्स (हेडर)', 'file_info_detected_type': 'फ़ाइल का प्रकार', 'file_info_unknown': 'अज्ञात डेटा', 'file_info_analyzing': 'विश्लेषण कर रहा है...'},
    'values-hu-rHU': {'file_info_title': 'Fájlinformáció Elemző', 'file_info_select_file_hint': 'Koppintson egy fájl kiválasztásához', 'file_info_magic_bytes': 'Mágikus bájtok (Fejléc)', 'file_info_detected_type': 'Észlelt fájltípus', 'file_info_unknown': 'Ismeretlen bináris adat', 'file_info_analyzing': 'Fájlfejléc elemzése...'},
    'values-in': {'file_info_title': 'Penganalisis Info File', 'file_info_select_file_hint': 'Ketuk untuk memilih file', 'file_info_magic_bytes': 'Byte Ajaib (Header)', 'file_info_detected_type': 'Jenis File Terdeteksi', 'file_info_unknown': 'Data Biner Tidak Dikenal', 'file_info_analyzing': 'Menganalisis Header File...'},
    'values-it-rIT': {'file_info_title': 'Analizzatore info file', 'file_info_select_file_hint': 'Tocca per selezionare un file', 'file_info_magic_bytes': 'Byte magici (Intestazione)', 'file_info_detected_type': 'Tipo di file rilevato', 'file_info_unknown': 'Dati binari sconosciuti', 'file_info_analyzing': 'Analisi dell\'intestazione...'},
    'values-iw': {'file_info_title': 'מנתח מידע קובץ', 'file_info_select_file_hint': 'הקש לבחירת קובץ לניתוח', 'file_info_magic_bytes': 'בייטים של קסם (כותרת)', 'file_info_detected_type': 'סוג קובץ שזוהה', 'file_info_unknown': 'נתונים בינאריים לא ידועים', 'file_info_analyzing': 'מנתח כותרת קובץ...'},
    'values-ja-rJP': {'file_info_title': 'ファイル情報アナライザ', 'file_info_select_file_hint': 'タップしてファイルを選択', 'file_info_magic_bytes': 'マジックバイト (ヘッダー)', 'file_info_detected_type': '検出されたタイプ', 'file_info_unknown': '不明なバイナリデータ', 'file_info_analyzing': 'ヘッダーを分析中...'},
    'values-nb-rNO': {'file_info_title': 'Filinfo Analysator', 'file_info_select_file_hint': 'Trykk for å velge en fil', 'file_info_magic_bytes': 'Magiske Bytes (Header)', 'file_info_detected_type': 'Oppdaget filtype', 'file_info_unknown': 'Ukjente binærdata', 'file_info_analyzing': 'Analyserer filhode...'},
    'values-nl-rNL': {'file_info_title': 'Bestandsinfo Analysator', 'file_info_select_file_hint': 'Tik om een bestand te selecteren', 'file_info_magic_bytes': 'Magische Bytes (Header)', 'file_info_detected_type': 'Gedetecteerd bestandstype', 'file_info_unknown': 'Onbekende binaire gegevens', 'file_info_analyzing': 'Bestandsheader analyseren...'},
    'values-pt-rBR': {'file_info_title': 'Analisador de Informações', 'file_info_select_file_hint': 'Toque para selecionar um arquivo', 'file_info_magic_bytes': 'Bytes Mágicos (Cabeçalho)', 'file_info_detected_type': 'Tipo de Arquivo Detectado', 'file_info_unknown': 'Dados Binários Desconhecidos', 'file_info_analyzing': 'Analisando cabeçalho...'},
    'values-pt-rPT': {'file_info_title': 'Analisador de Informações', 'file_info_select_file_hint': 'Toque para selecionar um ficheiro', 'file_info_magic_bytes': 'Bytes Mágicos (Cabeçalho)', 'file_info_detected_type': 'Tipo de Ficheiro Detetado', 'file_info_unknown': 'Dados Binários Desconhecidos', 'file_info_analyzing': 'A analisar cabeçalho...'},
    'values-ru-rRU': {'file_info_title': 'Анализатор информации о файле', 'file_info_select_file_hint': 'Нажмите, чтобы выбрать файл', 'file_info_magic_bytes': 'Магические байты (заголовок)', 'file_info_detected_type': 'Обнаруженный тип', 'file_info_unknown': 'Неизвестные данные', 'file_info_analyzing': 'Анализ заголовка...'},
    'values-tr-rTR': {'file_info_title': 'Dosya Bilgisi Analizcisi', 'file_info_select_file_hint': 'Analiz edilecek dosyayı seç', 'file_info_magic_bytes': 'Sihirli Baytlar (Başlık)', 'file_info_detected_type': 'Algılanan Dosya Türü', 'file_info_unknown': 'Bilinmeyen İkili Veri', 'file_info_analyzing': 'Dosya başlığı inceleniyor...'},
    'values-vi-rVN': {'file_info_title': 'Phân tích Thông tin File', 'file_info_select_file_hint': 'Chạm vào để chọn file', 'file_info_magic_bytes': 'Mã nhận diện (Magic Bytes)', 'file_info_detected_type': 'Định dạng nhận diện', 'file_info_unknown': 'Dữ liệu nhị phân không rõ', 'file_info_analyzing': 'Đang phân tích Header...'},
    'values-zh-rCN': {'file_info_title': '文件信息分析器', 'file_info_select_file_hint': '点击选择要分析的文件', 'file_info_magic_bytes': '魔数 (文件头)', 'file_info_detected_type': '检测到的文件类型', 'file_info_unknown': '未知的二进制数据', 'file_info_analyzing': '正在分析文件头...'}
}

base_dir = "/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260207HexViewer/app/src/main/res"

for folder, strings_dict in translations.items():
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already inserted
        if 'file_info_title' in content:
            print(f"Skipping {folder}, already updated.")
            continue
            
        insertion = "\n"
        for key, value in strings_dict.items():
            insertion += f'    <string name="{key}">{value}</string>\n'
            
        new_content = content.replace("</resources>", f"{insertion}</resources>")
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {folder}/strings.xml")

