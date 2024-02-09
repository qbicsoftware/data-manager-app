/*Vaadin currently supports no java native way to copy to clipboard, but provides a recipe in their cookbook
https://github.com/vaadin/cookbook/tree/main/frontend/recipe/copytoclipboard */
window.copyToClipboard = str => {
  /*Legacy implementation for older browsers */
  if (!navigator.clipboard) {
    const textarea = document.createElement('textarea');
    textarea.value = str;
    textarea.style.position = 'absolute';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  } else {
    /*Safari only allows to copy to clipboard via async promise which is why this timeout function is necessary for more detail see here:
    https://stackoverflow.com/questions/70179363/navigator-clipboard-copy-doesnt-work-on-safari-when-the-copy-text-is-grabbed-via */
    setTimeout(() => {
      navigator.clipboard.writeText(str)
    }, 0)
  }
}
