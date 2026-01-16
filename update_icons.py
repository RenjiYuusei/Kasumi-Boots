from PIL import Image
import os
import requests
from io import BytesIO

# Configuration
IMAGE_URL = "https://i.ibb.co/5WZqvKsh/Gemini-Generated-Image-ri6w8cri6w8cri6w-1.png"
BASE_RES_PATH = "app/src/main/res"

# Define sizes for standard Android mipmap folders
SIZES = {
    "mipmap-mdpi": (48, 48),
    "mipmap-hdpi": (72, 72),
    "mipmap-xhdpi": (96, 96),
    "mipmap-xxhdpi": (144, 144),
    "mipmap-xxxhdpi": (192, 192)
}

def update_icons():
    try:
        print(f"Downloading image from {IMAGE_URL}...")
        response = requests.get(IMAGE_URL)
        response.raise_for_status()

        img = Image.open(BytesIO(response.content))
        print("Image downloaded successfully.")

        for folder, size in SIZES.items():
            target_dir = os.path.join(BASE_RES_PATH, folder)
            os.makedirs(target_dir, exist_ok=True)

            # High-quality resize
            resized_img = img.resize(size, Image.Resampling.LANCZOS)

            # Save as ic_launcher.png (Main icon)
            target_path = os.path.join(target_dir, "ic_launcher.png")
            resized_img.save(target_path, "PNG")

            # Save as ic_launcher_round.png (Round icon)
            # ideally this should be a circular crop, but for now we use the same image
            target_path_round = os.path.join(target_dir, "ic_launcher_round.png")
            resized_img.save(target_path_round, "PNG")

            print(f"Updated {folder} ({size})")

        print("All icons updated successfully.")

    except Exception as e:
        print(f"Error updating icons: {e}")

if __name__ == "__main__":
    update_icons()
