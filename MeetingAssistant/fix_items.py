import os
import re

directory = r"E:\Skripsi\Aplikasi\MeetingAssistant\app\src\main\res\layout"

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find all TextViews and add maxLines="1" and ellipsize="end" if not present
    def replacer(match):
        tag = match.group(0)
        if 'android:maxLines' not in tag:
            tag = tag.replace('<TextView', '<TextView\n        android:maxLines="1"\n        android:ellipsize="end"')
        return tag
    
    new_content = re.sub(r'<TextView[^>]+>', replacer, content)

    # Also, ensure the LinearLayout holding these has a fixed padding or height if needed, 
    # but maxLines="1" should enforce a consistent single-line height across all rows.
    # To be absolutely sure they are exactly the same size, let's also set layout_height="wrap_content" 
    # on the CardView (which it already has) and let the single-line TextViews dictate the height.

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
    else:
        print(f"No changes for {filepath}")

for filename in os.listdir(directory):
    if filename.startswith("item_") and filename.endswith(".xml"):
        process_file(os.path.join(directory, filename))
