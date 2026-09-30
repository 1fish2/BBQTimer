#! /usr/bin/env python

# List of characters to display
# (Name, Hex Code)
chars = [
    ("Bell", "1F514"),
    ("Bell with Slash", "1F515"),
    ("Bellhop Bell", "1F6CE"),
    ("Watch", "231A"),
    ("Hourglass", "231B"),
    ("Alarm Clock", "23F0"),
    ("Stopwatch", "23F1"),
    ("Timer Clock", "23F2"),
] + [("clock", hex(h)) for h in range(0x1F550, 0x1F568)]  # every hour and half-hour

def to_java_literal(hex_str):
    # Java uses \uXXXX for Basic Multilingual Plane (BMP)
    # and surrogate pairs for higher planes.
    val = int(hex_str, 16)
    if val <= 0xFFFF:
        return f"\\u{val:04X}"
    else:
        # Calculate surrogate pairs for Java
        high = (val - 0x10000) // 0x400 + 0xD800
        low = (val - 0x10000) % 0x400 + 0xDC00
        return f"\\u{high:04X}\\u{low:04X}"

print(f"{'Character Name':<18} | {'Standard'} | {'Monochrome'} | {'Java Literal'} | {'XML Literal'}")
print("-" * 78)

for name, hex_code in chars:
    char = chr(int(hex_code, 16))
    mono = char + "\uFE0E"  # Append Variation Selector-15
    java = to_java_literal(hex_code)
    xml  = f"&#x{hex_code};"

    print(f"{name:<18} | {char:^8} | {mono:^10} | {java} | {xml}")

# ♩♪♫♬