#!/usr/bin/env python3
"""Convert README.md and README_de.md into Google Play and Fastlane store descriptions.

Google Play Store and IzzyOnDroid (via Fastlane) both enforce a strict 4,000-character
limit for the full description field (with IzzyOnDroid prepending short_description.txt
and a blank line). This script:
- Extracts the introductory overview, Tips, and System Settings sections.
- Excludes sections between [](no-store) and [](/no-store) comments for all stores, and
  additionally excludes sections between [](no-fastlane) and [](/no-fastlane) comments
  for Fastlane.
- Preserves the Markdown section ordering.
- Formats output via format-specific converter subclasses:
  * PlayStoreConverter: plain text with bullet characters (•), NBSP indents, and
    appended repository source code link for Play-assets/store-description*.txt.
  * FastlaneConverter: Markdown bullet lists (* and 4-space nested *), preceding blank
    lines before bullet lists, and basic HTML inline formatting (<b>, <i>, <br><b>)
    for fastlane/metadata/android/*/full_description.txt.
- Reports character counts and compares them to the 4,000-character limit.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
import argparse
from pathlib import Path
import re
import sys

STORE_MAX_CHARS = 4000

NBSP = "\u00a0"
BULLET = "•"

INTERVAL_HEADING_PATTERN = re.compile(
    r"^(?:Example intervals|Short forms|Beispiel-?Intervalle|Praktische Kurzformen):?$",
    re.IGNORECASE,
)


ALL_MARKER_TAGS = ("no-store", "no-fastlane")


def strip_inline_formatting(text: str) -> str:
    """Strip bold, italics, backticks, HTML tags, Markdown links, and unescape symbols."""
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
    # Convert Markdown links [text](url) -> text
    text = re.sub(r"\[([^]]+)]\([^)]+\)", r"\1", text)
    # Unescape escaped Markdown chars like \[ \] \*
    text = re.sub(r"\\([\\\[\]()*_`#])", r"\1", text)
    return text


class DescriptionConverter(ABC):
    """Base class for converting extracted README Markdown lines into store description formats."""

    store_name: str = "Store"
    exclude_tags: tuple[str, ...] = ("no-store",)
    needs_blank_line_before_list: bool = False
    include_repo_link: bool = False

    def extract_raw_sections(self, md_text: str) -> list[str]:
        """Extract intro, tips, and system settings lines from README text."""
        # Strip \s*[](tag) ... [](/tag) blocks for tags in self.exclude_tags to squeeze
        # descriptions into 4,000 chars.
        #
        # <!-- no-store --> ... <!-- /no-store --> would be more portable but JetBrains Markdown
        # preview hides it only if it's on a line by itself, and that alters bullet items.
        cleaned_md = md_text
        for tag in self.exclude_tags:
            cleaned_md = re.sub(
                rf"\s*\[]\({re.escape(tag)}\).*?\[]\(/{re.escape(tag)}\)",
                "",
                cleaned_md,
                flags=re.DOTALL | re.IGNORECASE,
            )

        # Strip any remaining marker tags (for other stores) while keeping their inner text.
        for tag in ALL_MARKER_TAGS:
            cleaned_md = re.sub(
                rf"\[]\(/?{re.escape(tag)}\)",
                "",
                cleaned_md,
                flags=re.IGNORECASE,
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
                    raw_lines.append(line)
                elif header in ("License", "Lizenz"):
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

    @abstractmethod
    def format_inline(self, text: str) -> str:
        """Format inline Markdown (bold, italics, code, links, HTML tags)."""

    @abstractmethod
    def format_heading(self, heading_text: str) -> str:
        """Format a section heading (e.g. ## Tips or ### System settings...)."""

    @abstractmethod
    def format_bullet_item(
        self, item_text: str, indent_level: int, in_interval_examples: bool
    ) -> str:
        """Format a bullet list item."""

    @abstractmethod
    def format_numbered_item(self, num: str, item_text: str, indent_level: int) -> str:
        """Format a numbered list item."""

    @abstractmethod
    def format_continuation(self, cont_text: str, indent_level: int) -> str:
        """Format an indented continuation line under a bullet item."""

    def extra_char_count(self, dst_path: Path) -> tuple[int, str]:
        """Return (extra_chars, description) added by the store pipeline, if any."""
        _ = dst_path
        return 0, ""

    def convert_lines(self, raw_lines: list[str], is_german: bool) -> str:
        """Convert extracted Markdown lines into the target format."""
        output_lines: list[str] = []
        in_interval_examples = False
        in_list = False

        for line in raw_lines:
            stripped = line.strip()
            if not stripped:
                in_interval_examples = False
                in_list = False
                output_lines.append("")
                continue

            # Check for headings (# ...)
            m_head = re.match(r"^(#{1,6})\s+(.*)$", stripped)
            if m_head:
                raw_heading = m_head.group(2).strip()
                plain_heading = strip_inline_formatting(raw_heading)
                heading_text = self.format_inline(raw_heading)
                output_lines.append(self.format_heading(heading_text))
                in_interval_examples = bool(INTERVAL_HEADING_PATTERN.match(plain_heading))
                in_list = False
                continue

            # Check if plain text line starts interval examples
            if INTERVAL_HEADING_PATTERN.match(stripped):
                in_interval_examples = True
                in_list = False
                output_lines.append(self.format_inline(stripped))
                continue

            # Check for bullet list item: (* or -)
            m_bullet = re.match(r"^(\s*)([*\-])\s+(.*)$", line)
            if m_bullet:
                if (
                    self.needs_blank_line_before_list
                    and not in_list
                    and output_lines
                    and output_lines[-1] != ""
                ):
                    output_lines.append("")
                in_list = True
                indent_spaces = len(m_bullet.group(1))
                indent_level = indent_spaces // 2
                item_text = self.format_inline(m_bullet.group(3).strip())
                output_lines.append(
                    self.format_bullet_item(item_text, indent_level, in_interval_examples)
                )
                continue

            # Check for numbered list item: (1. ...)
            m_num = re.match(r"^(\s*)(\d+\.)\s+(.*)$", line)
            if m_num:
                if (
                    self.needs_blank_line_before_list
                    and not in_list
                    and output_lines
                    and output_lines[-1] != ""
                ):
                    output_lines.append("")
                in_list = True
                indent_spaces = len(m_num.group(1))
                indent_level = max(1, indent_spaces // 2)
                num = m_num.group(2)
                item_text = self.format_inline(m_num.group(3).strip())
                output_lines.append(self.format_numbered_item(num, item_text, indent_level))
                continue

            # Check for continuation line indented under a bullet
            m_cont = re.match(r"^(\s{2,})(.*)$", line)
            if m_cont:
                indent_spaces = len(m_cont.group(1))
                indent_level = max(1, indent_spaces // 2)
                cont_text = self.format_inline(m_cont.group(2).strip())
                output_lines.append(self.format_continuation(cont_text, indent_level))
                continue

            # Regular paragraph line
            in_interval_examples = False
            in_list = False
            output_lines.append(self.format_inline(stripped))

        body = "\n".join(output_lines).strip()
        # Normalize excessive blank lines to double newlines
        body = re.sub(r"\n{3,}", "\n\n", body)

        if self.include_repo_link:
            repo_prefix = "Quellcode:" if is_german else "Source code:"
            repo_line = f"{repo_prefix} https://github.com/1fish2/BBQTimer"
            if repo_line not in body:
                return body + f"\n\n\n{repo_line}\n"

        return body + "\n"

    def convert_file(
        self, src_path: Path, dst_path: Path, is_german: bool, dry_run: bool = False
    ) -> int:
        """Read source Markdown, convert it, optionally write to destination, and report size."""
        if not src_path.is_file():
            print(f"Error: source file not found: {src_path}", file=sys.stderr)
            return 1

        content = src_path.read_text(encoding="utf-8")
        raw_lines = self.extract_raw_sections(content)
        result = self.convert_lines(raw_lines, is_german=is_german)

        char_count = len(result)
        extra_chars, extra_desc = self.extra_char_count(dst_path)
        total_chars = char_count + extra_chars
        diff_4000 = total_chars - STORE_MAX_CHARS

        status_4000 = (
            f"{abs(diff_4000)} chars under limit"
            if diff_4000 <= 0
            else f"{diff_4000} CHARS OVER LIMIT!"
        )
        status_indicator = "✓ OK" if diff_4000 <= 0 else "⚠ EXCEEDS LIMIT"

        print(f"[{status_indicator}] {src_path.name} -> {dst_path}")
        if extra_chars > 0:
            print(
                f"  Length: {char_count} characters"
                f" (+ {extra_chars} {extra_desc} = {total_chars} total)"
            )
        else:
            print(f"  Length: {char_count} characters")
        print(f"  vs {self.store_name} limit (4,000): {status_4000}")

        if not dry_run:
            dst_path.parent.mkdir(parents=True, exist_ok=True)
            dst_path.write_text(result, encoding="utf-8")
            print(f"  Wrote {char_count} characters to {dst_path}")

        return 0 if diff_4000 <= 0 else 2


class PlayStoreConverter(DescriptionConverter):
    """Converter for Google Play Store plain-text description files."""

    store_name = "Google Play"
    needs_blank_line_before_list = False
    include_repo_link = True

    def format_inline(self, text: str) -> str:
        return strip_inline_formatting(text)

    def format_heading(self, heading_text: str) -> str:
        return heading_text

    def format_bullet_item(
        self, item_text: str, indent_level: int, in_interval_examples: bool
    ) -> str:
        if in_interval_examples and indent_level == 0:
            return f"{NBSP}{item_text}"
        prefix = (NBSP * 2 * indent_level) + f"{BULLET} "
        return f"{prefix}{item_text}"

    def format_numbered_item(self, num: str, item_text: str, indent_level: int) -> str:
        prefix = NBSP * 2 * indent_level
        return f"{prefix}{num} {item_text}"

    def format_continuation(self, cont_text: str, indent_level: int) -> str:
        prefix = NBSP * 2 * indent_level
        return f"{prefix}{cont_text}"


class FastlaneConverter(DescriptionConverter):
    """Converter for Fastlane / IzzyOnDroid full_description.txt files."""

    store_name = "IzzyOnDroid"
    exclude_tags = ("no-store", "no-fastlane")
    needs_blank_line_before_list = True
    include_repo_link = False

    def format_inline(self, text: str) -> str:
        # Remove HTML tags (e.g. <span>, <img ...>)
        text = re.sub(r"</?[a-zA-Z0-9]+[^>]*>", "", text)
        # Convert Markdown bold: **text** or __text__ -> <b>text</b>
        text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
        text = re.sub(r"__(.+?)__", r"<b>\1</b>", text)
        # Convert Markdown italics: *text* or _text_ -> <i>text</i>
        text = re.sub(r"\*([^*]+?)\*", r"<i>\1</i>", text)
        text = re.sub(r"(?<!\w)_([^_]+?)_(?!\w)", r"<i>\1</i>", text)
        # Convert code backticks: `code` -> <i>code</i>
        text = re.sub(r"`([^`]+?)`", r"<i>\1</i>", text)
        # Convert Markdown links [text](url) -> text
        text = re.sub(r"\[([^]]+)]\([^)]+\)", r"\1", text)
        # Unescape escaped Markdown chars like \[ \] \*
        text = re.sub(r"\\([\\\[\]()*_`#])", r"\1", text)
        return text

    def format_heading(self, heading_text: str) -> str:
        return f"<br><b>{heading_text}</b>"

    def format_bullet_item(
        self, item_text: str, indent_level: int, in_interval_examples: bool
    ) -> str:
        _ = in_interval_examples
        prefix = ("    " * indent_level) + "* "
        return f"{prefix}{item_text}"

    def format_numbered_item(self, num: str, item_text: str, indent_level: int) -> str:
        prefix = "    " * indent_level
        return f"{prefix}{num} {item_text}"

    def format_continuation(self, cont_text: str, indent_level: int) -> str:
        prefix = "  " * indent_level
        return f"{prefix}{cont_text}"

    def extra_char_count(self, dst_path: Path) -> tuple[int, str]:
        short_path = dst_path.parent / "short_description.txt"
        if short_path.is_file():
            short_text = short_path.read_text(encoding="utf-8").rstrip("\r\n")
            # IzzyOnDroid prepends short_description.txt + a blank line ("\n\n")
            return len(short_text) + 2, "for short_description + blank line"
        return 0, ""


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert BBQTimer README files to Google Play and Fastlane store descriptions."
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
        help="Path to English Play Store description (default: Play-assets/store-description.txt)",
    )
    parser.add_argument(
        "--output-de",
        type=Path,
        default=Path("Play-assets/store-description_de.txt"),
        help="Path to German Play Store description (default: Play-assets/store-description_de.txt)",
    )
    parser.add_argument(
        "--fastlane-en",
        type=Path,
        default=Path("fastlane/metadata/android/en-US/full_description.txt"),
        help=(
            "Path to English Fastlane description "
            "(default: fastlane/metadata/android/en-US/full_description.txt)"
        ),
    )
    parser.add_argument(
        "--fastlane-de",
        type=Path,
        default=Path("fastlane/metadata/android/de/full_description.txt"),
        help=(
            "Path to German Fastlane description "
            "(default: fastlane/metadata/android/de/full_description.txt)"
        ),
    )
    parser.add_argument(
        "--check-only",
        action="store_true",
        help="Check character lengths without writing output files.",
    )

    args = parser.parse_args()

    play_converter = PlayStoreConverter()
    fastlane_converter = FastlaneConverter()

    print("Converting README files to Google Play and Fastlane descriptions...")
    print("-" * 65)

    codes = [
        play_converter.convert_file(
            args.readme_en, args.output_en, is_german=False, dry_run=args.check_only
        ),
    ]
    print()
    codes.append(
        play_converter.convert_file(
            args.readme_de, args.output_de, is_german=True, dry_run=args.check_only
        )
    )
    print()
    codes.append(
        fastlane_converter.convert_file(
            args.readme_en, args.fastlane_en, is_german=False, dry_run=args.check_only
        )
    )
    print()
    codes.append(
        fastlane_converter.convert_file(
            args.readme_de, args.fastlane_de, is_german=True, dry_run=args.check_only
        )
    )

    print("-" * 65)
    return max(codes)


if __name__ == "__main__":
    sys.exit(main())
