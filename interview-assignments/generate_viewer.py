#!/usr/bin/env python3
"""
Generate syntax-highlighted HTML viewers for source files.
Supports Java (syntax highlighting) and Markdown (full rendering + Mermaid diagrams).
"""
import sys
import os
import re
import html

# ---------------------------------------------------------------------------
# Java syntax highlighting
# ---------------------------------------------------------------------------
JAVA_KEYWORDS = {
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char',
    'class', 'const', 'continue', 'default', 'do', 'double', 'else', 'enum',
    'extends', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements',
    'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new',
    'package', 'private', 'protected', 'public', 'return', 'short', 'static',
    'strictfp', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws',
    'transient', 'try', 'void', 'volatile', 'while', 'true', 'false', 'null',
    'var', 'record', 'sealed', 'permits', 'yield', 'when'
}

JAVA_INLINE_RULES = [
    (r'//.*$', 'comment'),
    (r'"(?:\\.|[^"\\])*"', 'string'),
    (r"'(?:\\.|[^'\\])'", 'string'),
    (r'@\w+(?:\.\w+)*', 'annotation'),
    (r'\b\d+(?:\.\d+)?[lLfFdD]?\b', 'number'),
]


def escape_and_identify(text, is_java=True):
    if not is_java:
        return html.escape(text)
    parts = re.split(r'(\b\w+\b)', text)
    out = []
    for part in parts:
        if part in JAVA_KEYWORDS:
            out.append(f'<span class="keyword">{html.escape(part)}</span>')
        elif len(part) > 0 and part[0].isupper() and part.isidentifier():
            out.append(f'<span class="type">{html.escape(part)}</span>')
        else:
            out.append(html.escape(part))
    return ''.join(out)


def highlight_line(line, inline_rules, is_java=True):
    result = []
    pos = 0
    n = len(line)
    while pos < n:
        best_match = None
        best_end = -1
        best_kind = None
        for pattern, kind in inline_rules:
            m = re.compile(pattern).match(line, pos)
            if m and m.end() > best_end:
                best_match = m
                best_end = m.end()
                best_kind = kind

        if best_match:
            if best_match.start() > pos:
                result.append(escape_and_identify(line[pos:best_match.start()], is_java))
            text = html.escape(best_match.group(0))
            result.append(f'<span class="{best_kind}">{text}</span>')
            pos = best_end
        else:
            result.append(escape_and_identify(line[pos:], is_java))
            break
    return ''.join(result)


def highlight_java(lines):
    in_block_comment = False
    output = []
    for line in lines:
        if in_block_comment:
            end_idx = line.find('*/')
            if end_idx != -1:
                comment_part = html.escape(line[:end_idx + 2])
                rest = line[end_idx + 2:]
                highlighted_rest = highlight_line(rest, JAVA_INLINE_RULES, True)
                output.append(f'<span class="comment">{comment_part}</span>{highlighted_rest}')
                in_block_comment = False
            else:
                output.append(f'<span class="comment">{html.escape(line)}</span>')
        else:
            start_idx = line.find('/*')
            if start_idx != -1:
                before = line[:start_idx]
                highlighted_before = highlight_line(before, JAVA_INLINE_RULES, True)
                end_idx = line.find('*/', start_idx + 2)
                if end_idx != -1:
                    comment_part = html.escape(line[start_idx:end_idx + 2])
                    rest = line[end_idx + 2:]
                    highlighted_rest = highlight_line(rest, JAVA_INLINE_RULES, True)
                    output.append(f'{highlighted_before}<span class="comment">{comment_part}</span>{highlighted_rest}')
                else:
                    comment_part = html.escape(line[start_idx:])
                    output.append(f'{highlighted_before}<span class="comment">{comment_part}</span>')
                    in_block_comment = True
            else:
                output.append(highlight_line(line, JAVA_INLINE_RULES, True))
    return output


# ---------------------------------------------------------------------------
# Markdown rendering
# ---------------------------------------------------------------------------
def flush_para(buf, out):
    if buf:
        text = ' '.join(buf)
        out.append(f'<p>{render_inline(text)}</p>')
        buf.clear()


def render_markdown(lines):
    """Simple markdown-to-HTML renderer with Mermaid support."""
    out = []
    i = 0
    n = len(lines)

    in_code = False
    code_lang = None
    code_lines = []

    in_list = False
    list_type = None
    para_buf = []

    while i < n:
        line = lines[i]
        stripped = line.strip()

        # Code block fences
        if stripped.startswith('```'):
            flush_para(para_buf, out)
            if in_code:
                content = '\n'.join(code_lines)
                if code_lang == 'mermaid':
                    out.append(f'<div class="mermaid">{html.escape(content)}</div>')
                else:
                    out.append(f'<pre><code>{html.escape(content)}</code></pre>')
                in_code = False
                code_lang = None
                code_lines = []
            else:
                in_code = True
                code_lang = stripped[3:].strip() or None
                code_lines = []
            i += 1
            continue

        if in_code:
            code_lines.append(line)
            i += 1
            continue

        # Blank line
        if not stripped:
            flush_para(para_buf, out)
            if in_list:
                out.append(f'</{list_type}>')
                in_list = False
                list_type = None
            i += 1
            continue

        # List items
        if re.match(r'^[-*+]\s', stripped):
            flush_para(para_buf, out)
            if not in_list:
                list_type = 'ul'
                out.append('<ul>')
                in_list = True
            item_text = re.sub(r'^[-*+]\s+', '', stripped)
            out.append(f'<li>{render_inline(item_text)}</li>')
            i += 1
            continue

        if re.match(r'^\d+\.\s', stripped):
            flush_para(para_buf, out)
            if not in_list:
                list_type = 'ol'
                out.append('<ol>')
                in_list = True
            item_text = re.sub(r'^\d+\.\s+', '', stripped)
            out.append(f'<li>{render_inline(item_text)}</li>')
            i += 1
            continue

        # Headers
        m = re.match(r'^(#{1,6})\s+(.*)$', stripped)
        if m:
            flush_para(para_buf, out)
            level = len(m.group(1))
            text = m.group(2)
            out.append(f'<h{level}>{render_inline(text)}</h{level}>')
            i += 1
            continue

        # Horizontal rule
        if re.match(r'^---+|===+|\*\*\*+', stripped):
            flush_para(para_buf, out)
            out.append('<hr>')
            i += 1
            continue

        # Accumulate paragraph text
        para_buf.append(stripped)
        i += 1

    flush_para(para_buf, out)
    if in_list:
        out.append(f'</{list_type}>')

    return '\n'.join(out)


def render_inline(text):
    """Render inline markdown: code, bold, italic, links."""
    # Escape HTML first
    text = html.escape(text)
    # Links: [text](url)
    text = re.sub(
        r'\[([^\]]+)\]\(([^)]+)\)',
        lambda m: f'<a href="{html.escape(m.group(2))}" target="_blank">{m.group(1)}</a>',
        text
    )
    # Bold **text**
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    # Italic *text* (but not inside words, and not already processed bold)
    text = re.sub(r'(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)', r'<em>\1</em>', text)
    # Inline code `text`
    text = re.sub(r'`([^`]+)`', r'<code>\1</code>', text)
    return text


# ---------------------------------------------------------------------------
# CSS & HTML generation
# ---------------------------------------------------------------------------
CSS = '''
:root { --bg: #0d1117; --fg: #c9d1d9; --line-num: #484f58; --comment: #8b949e; --keyword: #ff7b72; --string: #a5d6ff; --number: #79c0ff; --type: #ffa657; --annotation: #d2a8ff; --code: #e6edf3; --link: #58a6ff; --bold: #e6edf3; --italic: #c9d1d9; --header: #79c0ff; --codeblock: #8b949e; }
body { margin: 0; font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif; background: var(--bg); color: var(--fg); }
.header { background: #161b22; padding: 16px 24px; border-bottom: 1px solid #30363d; display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 10; }
.header a { color: #58a6ff; text-decoration: none; font-size: 14px; }
.header a:hover { text-decoration: underline; }
.header h1 { margin: 0; font-size: 16px; font-weight: 600; color: #e6edf3; }
.container { padding: 24px; }
pre { margin: 0; background: var(--bg); border: 1px solid #30363d; border-radius: 6px; padding: 12px 16px; overflow-x: auto; font-size: 13px; line-height: 1.1; counter-reset: line; }
code { font-family: ui-monospace,SFMono-Regular,"SF Mono",Menlo,Consolas,"Liberation Mono",monospace; display: block; }
.line { display: block; counter-increment: line; white-space: pre; }
.line::before { content: counter(line); display: inline-block; width: 3em; margin-right: 1em; color: var(--line-num); text-align: right; user-select: none; }
.comment { color: var(--comment); font-style: italic; }
.keyword { color: var(--keyword); font-weight: 600; }
.string { color: var(--string); }
.number { color: var(--number); }
.type { color: var(--type); }
.annotation { color: var(--annotation); }

/* Markdown rendered styles */
.md-content { font-size: 15px; line-height: 1.6; max-width: 900px; }
.md-content h1, .md-content h2, .md-content h3, .md-content h4 { color: var(--header); margin-top: 28px; margin-bottom: 12px; border-bottom: 1px solid #30363d; padding-bottom: 6px; }
.md-content h1 { font-size: 26px; }
.md-content h2 { font-size: 22px; }
.md-content h3 { font-size: 18px; }
.md-content p { margin: 0 0 14px; }
.md-content ul, .md-content ol { margin: 0 0 14px; padding-left: 24px; }
.md-content li { margin-bottom: 6px; }
.md-content li code, .md-content p code { background: rgba(110,118,129,0.2); padding: 2px 5px; border-radius: 4px; font-size: 13px; font-family: ui-monospace,SFMono-Regular,SF Mono,Menlo,Consolas,Liberation Mono,monospace; color: var(--code); }
.md-content pre { margin: 14px 0; background: #161b22; padding: 14px 16px; border-radius: 8px; border: 1px solid #30363d; }
.md-content pre code { display: block; line-height: 1.4; font-size: 13px; color: var(--code); }
.md-content a { color: var(--link); text-decoration: none; }
.md-content a:hover { text-decoration: underline; }
.md-content hr { border: none; border-top: 1px solid #30363d; margin: 24px 0; }
.md-content strong { color: var(--bold); }
.md-content em { color: var(--italic); font-style: italic; }
.mermaid { background: #ffffff; border-radius: 8px; padding: 16px; margin: 16px 0; text-align: center; }
'''


def generate(input_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        raw = f.read()

    _, ext = os.path.splitext(input_path)
    filename = os.path.basename(input_path)
    output_path = input_path + '.html'

    is_md = ext.lower() == '.md'
    is_java = ext.lower() == '.java'

    if is_md:
        md_lines = raw.split('\n')
        body_content = f'<div class="md-content">{render_markdown(md_lines)}</div>'
        mermaid_script = '''
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<script>mermaid.initialize({startOnLoad:true,theme:'dark'});</script>
'''
    elif is_java:
        highlighted_lines = highlight_java(raw.split('\n'))
        lined_content = '\n'.join(f'<span class="line">{line}</span>' for line in highlighted_lines)
        body_content = f'<pre><code>{lined_content}</code></pre>'
        mermaid_script = ''
    else:
        body_content = f'<pre>{html.escape(raw)}</pre>'
        mermaid_script = ''

    out = f'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>{filename}</title>
<style>{CSS}</style>
</head>
<body>
<div class="header">
<h1>{filename}</h1>
<a href="../index.html">&#8592; Back to assignments</a>
</div>
<div class="container">
{body_content}
</div>
{mermaid_script}
</body>
</html>'''

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(out)
    print(f"Generated {output_path}")


if __name__ == '__main__':
    for path in sys.argv[1:]:
        if os.path.isfile(path):
            generate(path)
        else:
            print(f"Skip missing file: {path}")
