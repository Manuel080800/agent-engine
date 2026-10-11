// UTF-8 properties support separators, continuations and Java-style escapes.
export function parseProperties(source) {
  const messages = Object.create(null);
  let pending = "";
  for (const line of source.replace(/^\uFEFF/, "").split(/\r?\n/)) {
    const logical = pending + (pending ? line.trimStart() : line);
    if (/(?:^|[^\\])(?:\\\\)*\\$/.test(logical)) {
      pending = logical.slice(0, -1);
      continue;
    }
    pending = "";
    const text = logical.trimStart();
    if (!text || /^[#!]/.test(text)) continue;
    const match = text.match(/^((?:\\.|[^=:\s])+)\s*(?:[=:]\s*|\s+)?(.*)$/);
    if (!match) throw new Error("Invalid properties entry");
    const key = unescape(match[1]);
    if (Object.hasOwn(messages, key)) throw new Error(`Duplicate properties key: ${key}`);
    messages[key] = unescape(match[2]);
  }
  if (pending) throw new Error("Incomplete properties continuation");
  return messages;
}

function unescape(value) {
  return value.replace(/\\u([\da-fA-F]{4})|\\(.)/g, (_, unicode, character) =>
    unicode
      ? String.fromCharCode(parseInt(unicode, 16))
      : ({ n: "\n", r: "\r", t: "\t", f: "\f" }[character] ?? character),
  );
}
