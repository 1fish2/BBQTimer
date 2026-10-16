#!/usr/bin/env python3
"""Convert README.md and README_de.md into Google Play Store description files.

Google Play Store enforces a strict 4,000-character limit for the full description
field. This script:
- Extracts the introductory overview, Tips, and System Settings sections.
- Excludes sections between [](no-store) and [](/no-store) comments.
- Strips Markdown headers (#), images, HTML tags and comments, bold, italics, and backticks.
- Formats bullet lists using bullet characters (•) and non-breaking space (NBSP) indents.
- Formats interval examples with NBSP indentation and no bullet.
- Preserves the Markdown section ordering.
- Appends the repository source code link (which is not needed in README.md since that's viewed in
  the repository).
- Writes Play-assets/store-description.txt and Play-assets/store-description_de.txt.
- Reports character counts and compares them to the 4,000-character limit.
"""

from __future__ import annotations

import argparse
from pathlib import Path
import re
import sys

PLAY_STORE_MAX_CHARS = 4000

NBSP = "\u00a0"
BULLET = "•"

INTERVAL_HEADING_PATTERN = re.compile(
    r"^(?:Example intervals|Short forms|Beispiel-?Intervalle|Praktische Kurzformen):?$",
    re.IGNORECASE,
)


def strip_inline_formatting(text: str) -> str:
    """Strip bold, italics, backticks, HTML tags, Markdown links, and unescape symbols."""
    # Unescape escaped Markdown chars like \[ \] \*
    text = re.sub(r"\\([\\\[\]()*_`#])", r"\1", text)
    # Remove HTML tags (e.g. <b>, <i>, <br>, <span>, etc.)
    text = re.sub(r"</?[a-zA-Z0-9]+[^>]*>", "", text)
    # Remove Markdown bold: **text** or __text__
    text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
    text = re.sub(r"__(.+?)__", r"\1", text)
    # Remove Markdown italics: *text* or _text_ (ensure not within words)
    text = re.sub(r"\*([^*]+?)\*", r"\1", text)
    text = re.sub(r"(?<!\w)_([^_]+?)_(?!\w)", r"\1", text)
    # Remove code backticks: `code`
    text = re.sub(r"`([^`]+?)`", r"\1", text)
    # Convert Markdown links [text](url) -> text (or url if text is empty)
    text = re.sub(r"\[([^]]+)]\([^)]+\)", r"\1", text)
    return text


def extract_raw_sections(md_text: str) -> list[str]:
    """Extract intro, tips, and system settings lines from README text."""
    # Strip \s*[](no-store) ... [](/no-store) blocks to squeeze store-description.txt into 4,000 chars.
    #
    # <!-- no-store --> ... <!-- /no-store --> would be more portable but JetBrains Markdown preview
    # hides it only if it's on a line by itself, and that alters bullet items.
    #    "\s*<!--\s*no-store\s*-->.*?<!--\s*/no-store\s*-->"
    cleaned_md = re.sub(
        r"\s*\[]\(no-store\).*?\[]\(/no-store\)",
        "",
        md_text,
        flags=re.DOTALL | re.IGNORECASE,
    )
    # Strip any remaining standalone HTML comments
    cleaned_md = re.sub(r"<!--.*?-->", "", cleaned_md, flags=re.DOTALL)

    lines = cleaned_md.splitlines()
    raw_lines: list[str] = []
    mode = "intro"

    for line in lines:
        if line.startswith("## "):
            header = line[3:].strip()
            if header in ("Android Requirements", "Systemanforderungen"):
                mode = "ignore"
            elif header in ("Tips", "Tipps", "Usage Tips", "Tipps zur Bedienung"):
                mode = "tips"
                raw_lines.append(header)
            elif header in ("License", "Lizenz"):
                #mode = "done"
                break
            else:
                if mode == "tips":
                    mode = "ignore"
            continue

        if mode == "intro":
            # Omit top-level '# ' title and image badges
            if line.startswith("# "):
                continue
            if "<img" in line or "![" in line or re.match(r"^\s*\[<img", line):
                continue
            raw_lines.append(line)
        elif mode == "tips":
            raw_lines.append(line)

    return raw_lines


def convert_lines(raw_lines: list[str], is_german: bool) -> str:
    """Format extracted Markdown lines into Google Play Store plain text format."""
    output_lines: list[str] = []
    in_interval_examples = False

    for line in raw_lines:
        stripped = line.strip()
        if not stripped:
            in_interval_examples = False
            output_lines.append("")
            continue

        # Check for headings (# ...)
        m_head = re.match(r"^(#{1,6})\s+(.*)$", stripped)
        if m_head:
            heading_text = strip_inline_formatting(m_head.group(2).strip())
            output_lines.append(heading_text)
            in_interval_examples = bool(INTERVAL_HEADING_PATTERN.match(heading_text))
            continue

        # Check if plain text line starts interval examples
        if INTERVAL_HEADING_PATTERN.match(stripped):
            in_interval_examples = True
            output_lines.append(strip_inline_formatting(stripped))
            continue

        # Check for bullet list item: (* or -)
        m_bullet = re.match(r"^(\s*)([*\-])\s+(.*)$", line)
        if m_bullet:
            indent_spaces = len(m_bullet.group(1))
            item_text = strip_inline_formatting(m_bullet.group(3).strip())
            if in_interval_examples and indent_spaces == 0:
                output_lines.append(f"{NBSP}{item_text}")
            else:
                indent_level = indent_spaces // 2
                prefix = (NBSP * 2 * indent_level) + f"{BULLET} "
                output_lines.append(f"{prefix}{item_text}")
            continue

        # Check for numbered list item: (1. ...)
        m_num = re.match(r"^(\s*)(\d+\.)\s+(.*)$", line)
        if m_num:
            indent_spaces = len(m_num.group(1))
            num = m_num.group(2)
            item_text = strip_inline_formatting(m_num.group(3).strip())
            indent_level = max(1, indent_spaces // 2)
            prefix = NBSP * 2 * indent_level
            output_lines.append(f"{prefix}{num} {item_text}")
            continue

        # Check for continuation line indented under a bullet
        m_cont = re.match(r"^(\s{2,})(.*)$", line)
        if m_cont:
            indent_spaces = len(m_cont.group(1))
            cont_text = strip_inline_formatting(m_cont.group(2).strip())
            indent_level = max(1, indent_spaces // 2)
            prefix = NBSP * 2 * indent_level
            output_lines.append(f"{prefix}{cont_text}")
            continue

        # Regular paragraph line
        in_interval_examples = False
        output_lines.append(strip_inline_formatting(stripped))

    # Append source code repository URL if not already present
    repo_prefix = "Quellcode:" if is_german else "Source code:"
    repo_line = f"{repo_prefix} https://github.com/1fish2/BBQTimer"

    body = "\n".join(output_lines).strip()
    # Normalize excessive blank lines to double newlines
    body = re.sub(r"\n{3,}", "\n\n", body)

    if repo_line not in body:
        result = body + f"\n\n\n{repo_line}\n"
    else:
        result = body + "\n"

    return result


def convert_file(src_path: Path, dst_path: Path, is_german: bool, dry_run: bool = False) -> int:
    """Read source Markdown, convert it, optionally write to destination, and report size."""
    if not src_path.is_file():
        print(f"Error: source file not found: {src_path}", file=sys.stderr)
        return 1

    content = src_path.read_text(encoding="utf-8")
    raw_lines = extract_raw_sections(content)
    result = convert_lines(raw_lines, is_german=is_german)

    char_count = len(result)
    diff_4000 = char_count - PLAY_STORE_MAX_CHARS

    status_4000 = f"{abs(diff_4000)} chars under limit" if diff_4000 <= 0 else f"{diff_4000} CHARS OVER LIMIT!"

    status_indicator = "✓ OK" if diff_4000 <= 0 else "⚠ EXCEEDS LIMIT"

    print(f"[{status_indicator}] {src_path.name} -> {dst_path}")
    print(f"  Length: {char_count} characters")
    print(f"  vs Google Play limit (4,000): {status_4000}")

    if not dry_run:
        dst_path.parent.mkdir(parents=True, exist_ok=True)
        dst_path.write_text(result, encoding="utf-8")
        print(f"  Wrote {char_count} characters to {dst_path}")

    return 0 if diff_4000 <= 0 else 2


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert BBQTimer README files to Google Play Store description format."
    )
    parser.add_argument(
        "--readme-en",
        type=Path,
        default=Path("README.md"),
        help="Path to English README (default: README.md)",
    )
    parser.add_argument(
        "--readme-de",
        type=Path,
        default=Path("README_de.md"),
        help="Path to German README (default: README_de.md)",
    )
    parser.add_argument(
        "--output-en",
        type=Path,
        default=Path("Play-assets/store-description.txt"),
        help="Path to English store description (default: Play-assets/store-description.txt)",
    )
    parser.add_argument(
        "--output-de",
        type=Path,
        default=Path("Play-assets/store-description_de.txt"),
        help="Path to German store description (default: Play-assets/store-description_de.txt)",
    )
    parser.add_argument(
        "--check-only",
        action="store_true",
        help="Check character lengths without writing output files.",
    )

    args = parser.parse_args()

    print("Converting README files to Google Play Store descriptions...")
    print("-" * 65)

    code_en = convert_file(args.readme_en, args.output_en, is_german=False, dry_run=args.check_only)
    print()
    code_de = convert_file(args.readme_de, args.output_de, is_german=True, dry_run=args.check_only)

    print("-" * 65)
    return max(code_en, code_de)


if __name__ == "__main__":
    sys.exit(main())
