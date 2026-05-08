/**
 * DOM 序列化器
 * 将 DomNode 树转为 LLM 可读的文本格式
 * 获取页面信息
 */

import type { DomNode } from './dom-builder';

/** 页面信息 */
export interface PageInfo {
  /** 当前页面 URL */
  url: string;
  /** 页面标题 */
  title: string;
  /** 窗口宽度 */
  windowWidth: number;
  /** 窗口高度 */
  windowHeight: number;
  /** 水平滚动位置 */
  scrollX: number;
  /** 垂直滚动位置 */
  scrollY: number;
}

/** 保留的语义标签 */
const SEMANTIC_TAGS = new Set(['nav', 'menu', 'header', 'footer', 'aside', 'dialog', 'form']);

/**
 * 将 DomNode 树序列化为 LLM 可读的文本
 *
 * 输出格式：
 * - 可交互元素：`[index]<tag attrs>text</tag>` 或 `[index]<tag attrs />`
 * - 不可交互的语义容器：`<nav>...</nav>`（保留结构上下文）
 * - 普通文本：直接输出文本行
 * - 缩进表示父子关系
 *
 * 示例：
 * ```
 * [0]<button type=submit>登录</button>
 * [1]<input type=text placeholder=请输入用户名/>
 *   [2]<label for=password>密码</label>
 * ```
 */
export function flatTreeToString(root: DomNode): string {
  const result: string[] = [];
  processNode(root, 0, result);
  return result.join('\n');
}

function processNode(node: DomNode, depth: number, result: string[]): void {
  const indent = '\t'.repeat(depth);

  if (node.type === 'text') {
    // 文本节点：只有在没有可交互父元素时才直接输出
    if (node.textContent) {
      result.push(`${indent}${node.textContent}`);
    }
    return;
  }

  // 元素节点
  if (!node.tagName) return;

  const isSemantic = SEMANTIC_TAGS.has(node.tagName);
  let nextDepth = depth;

  // 可交互元素：输出带索引的格式
  if (node.isInteractive && node.highlightIndex !== undefined) {
    nextDepth = depth + 1;

    const indexStr = `[${node.highlightIndex}]`;
    const attrsStr = buildAttrsString(node.attributes);
    const text = node.textContent?.trim() || '';

    let line = `${indent}${indexStr}<${node.tagName}`;
    if (attrsStr) line += ` ${attrsStr}`;

    if (text) {
      line += `>${text}</${node.tagName}>`;
    } else {
      line += ' />';
    }

    result.push(line);
  }

  // 语义标签（不可交互但保留结构上下文）
  const emitSemantic = isSemantic && node.highlightIndex === undefined;
  const mark = emitSemantic ? result.length : -1;

  if (emitSemantic) {
    result.push(`${indent}<${node.tagName}>`);
    nextDepth = depth + 1;
  }

  // 递归处理子节点
  if (node.children) {
    for (const child of node.children) {
      processNode(child, nextDepth, result);
    }
  }

  // 语义标签闭合
  if (emitSemantic) {
    // 空标签移除
    if (result.length === mark + 1) {
      result.pop();
    } else {
      result.push(`${indent}</${node.tagName}>`);
    }
  }
}

/**
 * 构建属性字符串
 */
function buildAttrsString(attrs?: Record<string, string>): string {
  if (!attrs || Object.keys(attrs).length === 0) return '';

  // 去重：属性值超过5字符的重复值只保留一个
  const filtered = { ...attrs };
  const keys = Object.keys(filtered);
  if (keys.length > 1) {
    const seenValues: Record<string, string> = {};
    for (const key of keys) {
      const value = filtered[key];
      if (value.length > 5) {
        if (value in seenValues) {
          delete filtered[key];
        } else {
          seenValues[value] = key;
        }
      }
    }
  }

  // 去掉与tagName重复的role
  // (在 dom-builder 中已处理，此处不再重复)

  // 移除与文本内容重复的属性
  // (简化处理，不在此处判断文本)

  return Object.entries(filtered)
    .map(([key, value]) => `${key}=${value}`)
    .join(' ');
}

/**
 * 获取当前页面信息
 */
export function getPageInfo(): PageInfo {
  return {
    url: window.location.href,
    title: document.title,
    windowWidth: window.innerWidth,
    windowHeight: window.innerHeight,
    scrollX: Math.round(window.scrollX || document.documentElement.scrollLeft || 0),
    scrollY: Math.round(window.scrollY || document.documentElement.scrollTop || 0),
  };
}
