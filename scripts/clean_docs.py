import re

def clean_readme():
    filepath = '/home/rushi/Desktop/projects/vittify/README.md'
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    skip_mode = False
    
    for line in lines:
        # Remove markdown comments entirely
        if line.startswith('[//]: #'):
            continue
            
        # Remove badges and share links
        if '[![Share]' in line or '[![GitHub' in line or '[![Vittify Banner]' in line:
            continue
            
        # Remove specific sentences/links
        if 'More banks being added regularly! [Request your bank' in line:
            new_lines.append('More banks being added regularly!\n')
            continue
            
        if 'git clone https://github' in line:
            continue # I'll just remove the git clone line entirely or let them figure it out, no, wait they might have it locally.
            
        # Remove community links
        if 'Open an issue]' in line:
            continue
            
        # Remove contributors table and footer entirely
        if '## Contributors' in line:
            skip_mode = True
            continue
            
        if skip_mode and '## Acknowledgements' in line:
            skip_mode = False
            
        if skip_mode:
            continue
            
        if '<a href="https://github.com' in line:
            continue
            
        new_lines.append(line)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
def clean_contributing():
    filepath = '/home/rushi/Desktop/projects/vittify/CONTRIBUTING.md'
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_lines = []
    
    for line in lines:
        if line.startswith('[//]: #'):
            continue
        if 'https://github.com' in line:
            line = re.sub(r'\[(.*?)\]\(https://github.com/.*?\)', r'\1', line)
        if 'git clone https://github' in line:
            continue
            
        new_lines.append(line)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
def clean_security():
    filepath = '/home/rushi/Desktop/projects/vittify/SECURITY.md'
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_lines = []
    for line in lines:
        if 'discord.gg' in line:
            line = re.sub(r'\[(.*?)\]\(https://discord.gg/.*?\)', '', line)
        new_lines.append(line)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
if __name__ == "__main__":
    clean_readme()
    clean_contributing()
    clean_security()
