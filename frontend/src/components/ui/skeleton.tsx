import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

/** Skeleton — a pulsing placeholder block shown while data loads. */
function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-pulse rounded-md bg-muted", className)}
      {...props}
    />
  );
}

export { Skeleton };
