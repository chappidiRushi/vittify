import os
import re

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        return

    original = content

    # Replace case-sensitive
    content = content.replace("Vittify", "Vittify")
    content = content.replace("vittify", "vittify")
    content = content.replace("VITTIFY", "VITTIFY")
    content = content.replace("Vittify", "Vittify")
    content = content.replace("Vittify", "Vittify")
    content = content.replace("vittify", "vittify")
    content = content.replace("VITTIFY", "VITTIFY")
    
    # Also fix "vittify" if it exists
    content = content.replace("Vittify", "Vittify")
    content = content.replace("vittify", "vittify")

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

def main():
    root_dir = '/home/rushi/Desktop/projects/vittify'
    ignore_dirs = {'.git', '.gradle', 'build', 'node_modules', '.idea'}

    for dirpath, dirnames, filenames in os.walk(root_dir):
        dirnames[:] = [d for d in dirnames if d not in ignore_dirs]
        for filename in filenames:
            if filename.endswith(('.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico', '.jar', '.apk', '.aab', '.class', '.bin')):
                continue
            filepath = os.path.join(dirpath, filename)
            process_file(filepath)

if __name__ == "__main__":
    main()
