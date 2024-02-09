/*Vaadin currently supports no java native way to copy to clipboard, but provides a recipe in their cookbook
https://github.com/vaadin/cookbook/tree/main/frontend/recipe/copytoclipboard */
window.copyToClipboard = str => {
  const textarea = document.createElement('textarea');
  textarea.value = str;
  textarea.style.position = 'absolute';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}
