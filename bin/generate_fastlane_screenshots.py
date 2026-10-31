#!/usr/bin/env python3
from __future__ import annotations
# Regenerate the fastlane screenshot files.
# ASSUMES: ImageMagick is installed.

import argparse
from pathlib import Path
import shutil
import subprocess
from typing import Dict, Optional, Sequence, Tuple

TIMEOUT = 60  # seconds


def find_imagemagick() -> str:
    """Find the ImageMagick executable on system PATH."""
    for cmd in ("magick", "convert"):
        if shutil.which(cmd):
            return cmd
    raise FileNotFoundError(
        "ImageMagick is not installed or not found on PATH. "
        "Please install ImageMagick (e.g., `brew install imagemagick`)."
    )


def run_cmd2(
        tokens: Sequence[str],
        trim: bool = True,
        timeout: Optional[int] = TIMEOUT,
        env: Optional[dict[str, str]] = None,
        input_: Optional[str] = None) -> Tuple[str, str]:
    """Run a shell command-line (in token list form) and return a tuple
    containing its (stdout, stderr).
    This does not expand filename patterns or environment variables or do other
    shell processing steps.

    Args:
        tokens: The command line as a list of string tokens.
        trim: Whether to trim trailing whitespace from the output. This is
            useful because the output usually ends with a newline.
        timeout: timeout in seconds; None for no timeout.
        env: optional environment variables for the new process to use instead
            of inheriting the current process' environment.
        input_: input for any prompts that may appear (passed to the subprocess' stdin)
    Returns:
        The command's stdout and stderr strings.
    Raises:
        OSError (e.g. FileNotFoundError [Python 3] or PermissionError),
          subprocess.SubprocessError (TimeoutExpired or CalledProcessError)
    """
    out = subprocess.run(
        list(tokens),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
        env=env,
        encoding='utf-8',
        timeout=timeout,
        input=input_)
    if trim:
        return out.stdout.rstrip(), out.stderr.rstrip()
    return out.stdout, out.stderr


def run_cmd(
        tokens: Sequence[str],
        trim: bool = True,
        timeout: Optional[int] = TIMEOUT,
        env: Optional[dict[str, str]] = None,
        input_: Optional[str] = None,
        verbose: bool = True) -> str:
    """Run a shell command-line (in token list form), print tokens and any stderr,
    then return the stdout.
    """
    if verbose:
        print(f"Executing: {' '.join(tokens)}")

    stdout, stderr = run_cmd2(tokens, trim=trim, timeout=timeout, env=env, input_=input_)

    if stderr and verbose:
        print(f'stderr={stderr}')
    return stdout


def resize_image_file(
        im_tool: str,
        src_file: Path,
        dst_file: Path,
        scale: float,
        quality: int = 90,
        verbose: bool = True) -> str:
    """Resize an image file and convert the format per dst_file."""
    output_options = ["-alpha", "off", "-quality", str(quality)]
    if scale != 1.0:
        output_options.extend(['-resize', f'{scale * 100:.3f}%'])

    tokens = [im_tool, str(src_file), *output_options, str(dst_file)]
    return run_cmd(tokens, verbose=verbose)


def resize_all(
        dry_run: bool = False,
        verbose: bool = True,
        selected_locales: Optional[Sequence[str]] = None) -> None:
    """Resize and convert the images."""
    im_tool = find_imagemagick()
    if verbose:
        print(f"Using ImageMagick tool: '{im_tool}'")

    forms = [
        ('Pixel-8a-screenshots', 'phoneScreenshots', 1),  # 736/2160?
        ('Pixel-Tablet-screenshots', 'tenInchScreenshots', 1),  # 1047/2560?
        ('Watch-screenshots', 'wearScreenshots', 1),
     ]
    locales = selected_locales or ['en-US', 'de']

    for (src_form, dst_form, scale_factor) in forms:
        for locale in locales:
            src_dir = Path('Play-assets') / locale / src_form
            dst_dir = Path('fastlane/metadata/android') / locale / 'images' / dst_form

            print(f"\nProcessing [{locale}] {src_form} -> {dst_form}")
            print(f"  SOURCE:      {src_dir}")
            print(f"  DESTINATION: {dst_dir}")

            if not src_dir.exists():
                print(f"  WARNING: Source directory does not exist: {src_dir}")
                continue

            if not dry_run:
                dst_dir.mkdir(parents=True, exist_ok=True)

            # Clean up existing destination JPGs
            for file in dst_dir.glob("*.jpg"):
                if dry_run:
                    print(f"  [DRY-RUN] Would remove: {file}")
                else:
                    if verbose:
                        print(f"  Removing old screenshot: {file.name}")
                    file.unlink()

            src_paths = sorted(src_dir.glob("*.png"))
            if not src_paths:
                print(f"  WARNING: No PNG screenshots found in {src_dir}")
                continue

            for i, src_path in enumerate(src_paths):
                dst_path = dst_dir / f'{i:02d}.jpg'

                if dry_run:
                    print(
                        f"  [DRY-RUN] Would convert: {src_path.name} -> {dst_path.name} "
                        f"(scale={scale_factor})"
                    )
                else:
                    resize_image_file(im_tool, src_path, dst_path, scale_factor, verbose)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Regenerate Fastlane Android screenshot image files."
    )
    parser.add_argument(
        "-n",
        "--dry-run",
        action="store_true",
        help="Show what actions would be taken without modifying files.",
    )
    parser.add_argument(
        "-q",
        "--quiet",
        action="store_true",
        help="Reduce verbose output logging.",
    )
    parser.add_argument(
        "-l",
        "--locales",
        nargs="+",
        help="Specific locales to process (e.g., en-US de). Defaults to all.",
    )

    args = parser.parse_args()
    resize_all(
        dry_run=args.dry_run,
        verbose=not args.quiet,
        selected_locales=args.locales,
    )


if __name__ == '__main__':
    main()
