#! /usr/bin/env python
# Regenerate the fastlane screenshot files.
# ASSUMES: ImageMagick is installed.

import subprocess
from typing import Optional, Sequence, Tuple
import os

TIMEOUT = 60  # seconds

SRC="Play-assets"
DST="fastlane/metadata/android"


def run_cmd2(tokens, trim=True, timeout=TIMEOUT, env=None, input_=None):
    # type: (Sequence[str], bool, Optional[int], Optional[dict], Optional[str]) -> Tuple[str, str]
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
        tokens,
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


def run_cmd(tokens, trim=True, timeout=TIMEOUT, env=None, input_=None):
    # type: (Sequence[str], bool, Optional[int], Optional[dict], Optional[str]) -> str
    """Run a shell command-line (in token list form), print tokens and any stderr,
    then return the stdout.
    """
    print(tokens)

    stdout, stderr = run_cmd2(tokens, trim=trim, timeout=timeout, env=env, input_=input_)

    if stderr:
        print(f'stderr={stderr}')
    return stdout


def resize_image_file(src_file: str, dst_file: str, scale: float) -> str:
    """This will resize an image file and convert the format per dst_file."""
    input_options = ()
    # Size and other adjustments to scale and keep the aspect ratio.
    # output_options = ('-resize', '100x100^' '-gravity', 'center', '-extent', '100x100')
    output_options = ('-resize', f'{scale * 100:.3f}%')
    tokens = ['convert', *input_options, src_file, *output_options, dst_file]
    return run_cmd(tokens)


def resize_batch(phone: bool = True, en: bool = True) -> None:
    """Resize a batch of images and convert them to jpeg format."""
    src_model = 'Pixel 3' if phone else 'Pixel C'
    dst_model = 'phone' if phone else 'tenInch'
    scale = 736/2160 if phone else 1047/2560
    src_locale = '' if en else 'de '
    dst_locale = 'en-US' if en else 'de'
    src_dir = f'{SRC}/{src_model} {src_locale}screenshots'
    dst_dir = f'{DST}/{dst_locale}/images/{dst_model}Screenshots'
    src_files = list(os.listdir(src_dir))
    png_files = list(filter(lambda e: e.endswith('.png'), src_files))
    print(f'SOURCE: {src_dir}; DESTINATION: {dst_dir}')
    for i, src_file in enumerate(png_files):
        resize_image_file(f'{src_dir}/{src_file}', f'{dst_dir}/{i:02d}.jpg', scale)

if __name__ == '__main__':
    resize_batch(True, True)
    resize_batch(False, True)
    resize_batch(True, False)
    resize_batch(False, False)
