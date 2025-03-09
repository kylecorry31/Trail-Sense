import os

script_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = f"{script_dir}/.."

pages = [
    # Animals - Mammals
    {
        "file": "app/src/main/res/raw/field_guide_mouse.txt",
        "image": "field_guide/mouse.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_rabbit.txt",
        "image": "field_guide/rabbit.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_squirrel.txt",
        "image": "field_guide/squirrel.webp"
    },

    # Animals - Birds
    {
        "file": "app/src/main/res/raw/field_guide_grouse.txt",
        "image": "field_guide/grouse.webp"
    },

    # Animals - Fish
    {
        "file": "app/src/main/res/raw/field_guide_black_bass.txt",
        "image": "field_guide/black_bass.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_carp.txt",
        "image": "field_guide/carp.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_sunfish.txt",
        "image": "field_guide/sunfish.webp"
    },

    # Animals - Crustaceans
    {
        "file": "app/src/main/res/raw/field_guide_crab.txt",
        "image": "field_guide/crab.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_crayfish.txt",
        "image": "field_guide/crayfish.webp"
    },

    # Animals - Mollusks
    {
        "file": "app/src/main/res/raw/field_guide_clam.txt",
        "image": "field_guide/clam.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_mussel.txt",
        "image": "field_guide/mussel.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_periwinkle.txt",
        "image": "field_guide/periwinkle.webp"
    },

    # Animals - Insects
    {
        "file": "app/src/main/res/raw/field_guide_ant.txt",
        "image": "field_guide/ant.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_cricket.txt",
        "image": "field_guide/cricket.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_grasshopper.txt",
        "image": "field_guide/grasshopper.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_grub.txt",
        "image": "survival_guide/grub.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_termite.txt",
        "image": "field_guide/termite.webp"
    },

    # Animals - Worms
    {
        "file": "app/src/main/res/raw/field_guide_earthworm.txt",
        "image": "field_guide/earthworm.webp"
    },

    # Plants
    {
        "file": "app/src/main/res/raw/field_guide_bamboo.txt",
        "image": "field_guide/bamboo.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_brambles.txt",
        "image": "field_guide/brambles.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_cattail.txt",
        "image": "field_guide/cattail.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_clover.txt",
        "image": "field_guide/clover.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_common_plantain.txt",
        "image": "field_guide/common_plantain.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_dandelion.txt",
        "image": "field_guide/dandelion.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_dock.txt",
        "image": "field_guide/dock.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_kelp.txt",
        "image": "field_guide/kelp.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_poison_ivy.txt",
        "image": "survival_guide/poison_ivy.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_stinging_nettle.txt",
        "image": "survival_guide/stinging_nettle.webp"
    },

    # Fungi
    {
        "file": "app/src/main/res/raw/field_guide_bolete.txt",
        "image": "field_guide/bolete.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_chaga.txt",
        "image": "survival_guide/chaga.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_chicken_of_the_woods.txt",
        "image": "field_guide/chicken_of_the_woods.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_morel.txt",
        "image": "field_guide/morel.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_oyster_mushroom.txt",
        "image": "field_guide/oyster_mushroom.webp"
    },
    {
        "file": "app/src/main/res/raw/field_guide_tinder_fungus.txt",
        "image": "survival_guide/tinder_fungus.webp"
    },

    # Rocks
    {
        "file": "app/src/main/res/raw/field_guide_chert.txt",
        "image": "field_guide/chert.webp"
    }
]

content = "This chapter contains some common animals, plants, fungi, and rocks that may be useful in a survival situation. Always use caution when identifying plants, animals, or mushrooms. Some species may be dangerous or protected. If you are unsure about an identification, consult a professional.\n\n"

for page in pages:
    with open(f"{root_dir}/{page['file']}", 'r') as file:
        text = file.read()
        file_content = '\\pagebreak\n\n## ' + text.split('\n')[0] + f'\n\n![](file:///android_asset/{page['image']})\n\n' + '\n'.join(text.split('\n')[1:])

        # Remove any line that starts with http
        file_content = '\n'.join([line for line in file_content.split('\n') if not line.startswith('http')])

        file_content = file_content.strip()
        content += '\n\n' + file_content

content = content.strip()

with open("field_guide.md", 'w') as file:
    file.write(content)
