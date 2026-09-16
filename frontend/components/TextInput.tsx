"use client";

import type { InputHTMLAttributes, TextareaHTMLAttributes } from "react";
import { controlClass } from "./Field";

interface Common {
  invalid?: boolean;
  onValueChange?: (value: string) => void;
}

export type TextInputProps = Common & Omit<InputHTMLAttributes<HTMLInputElement>, "onChange">;

// 한 줄 입력. 높이 38, 모서리 4
export function TextInput({ invalid, onValueChange, className = "", ...rest }: TextInputProps) {
  return (
    <input
      aria-invalid={invalid || undefined}
      onChange={(e) => onValueChange?.(e.target.value)}
      className={controlClass(invalid, className)}
      {...rest}
    />
  );
}

export type TextAreaProps = Common & Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, "onChange">;

// 여러 줄 입력. 설명(2,000자)과 취소 사유(300자)
export function TextArea({ invalid, onValueChange, className = "", rows = 4, ...rest }: TextAreaProps) {
  return (
    <textarea
      rows={rows}
      aria-invalid={invalid || undefined}
      onChange={(e) => onValueChange?.(e.target.value)}
      className={controlClass(invalid, `h-auto py-2 ${className}`)}
      {...rest}
    />
  );
}
