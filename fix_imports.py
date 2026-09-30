import os

fixes = {
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/settings/cloudbackup/BackupSyncScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCheckbox",
         "import com.reddy.vittify.presentation.ui.components.VittifyCheckbox"),
    ],
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/settings/dataprivacy/DataPrivacyScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCheckbox",
         "import com.reddy.vittify.presentation.ui.components.VittifyCheckbox"),
    ],
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/settings/rules/RulesScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCard",
         "import com.reddy.vittify.presentation.ui.components.VittifyCard"),
    ],
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/settings/unrecognized/UnrecognizedSmsScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCard",
         "import com.reddy.vittify.presentation.ui.components.VittifyCard"),
    ],
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/settings/webhooks/WebhooksScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCard",
         "import com.reddy.vittify.presentation.ui.components.VittifyCard"),
    ],
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/subscriptions/SubscriptionsScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCard",
         "import com.reddy.vittify.presentation.ui.components.VittifyCard"),
    ],
    "/home/rushi/Desktop/projects/Vittify/app/src/main/java/com/reddy/vittify/presentation/ui/features/transactions/TransactionDetailScreen.kt": [
        ("import com.reddy.vittify.presentation.ui.components.VittifyCheckbox",
         "import com.reddy.vittify.presentation.ui.components.VittifyCheckbox"),
        ("import com.reddy.vittify.presentation.ui.components.VittifyCard",
         "import com.reddy.vittify.presentation.ui.components.VittifyCard"),
    ],
}

# Also do a broad scan and fix any other files with these stale imports
src_root = "/home/rushi/Desktop/projects/Vittify/app/src"

stale_imports = {
    "import com.reddy.vittify.presentation.ui.components.VittifyCard":
        "import com.reddy.vittify.presentation.ui.components.VittifyCard",
    "import com.reddy.vittify.presentation.ui.components.VittifyCheckbox":
        "import com.reddy.vittify.presentation.ui.components.VittifyCheckbox",
    "import com.reddy.vittify.presentation.ui.theme.VittifyTheme":
        "import com.reddy.vittify.presentation.ui.theme.VittifyTheme",
    "import com.reddy.vittify.presentation.ui.icons.VittifyOutline":
        "import com.reddy.vittify.presentation.ui.icons.VittifyOutline",
    "import com.reddy.vittify.presentation.navigation.VittifyBottomNavigation":
        "import com.reddy.vittify.presentation.navigation.VittifyBottomNavigation",
    "import com.reddy.vittify.presentation.navigation.VittifyDestinations":
        "import com.reddy.vittify.presentation.navigation.VittifyDestinations",
    "import com.reddy.vittify.data.backup.VittifyBackup":
        "import com.reddy.vittify.data.backup.VittifyBackup",
}

total_fixed = 0
for dirpath, dirnames, filenames in os.walk(src_root):
    for fname in filenames:
        if not fname.endswith('.kt'):
            continue
        fpath = os.path.join(dirpath, fname)
        try:
            with open(fpath, 'r', encoding='utf-8') as f:
                content = f.read()
            new_content = content
            for old, new in stale_imports.items():
                new_content = new_content.replace(old, new)
            if new_content != content:
                with open(fpath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                total_fixed += 1
                print(f"Fixed: {fpath}")
        except Exception as e:
            print(f"Error {fpath}: {e}")

print(f"\nTotal files fixed: {total_fixed}")
