import { useState, useRef, useEffect, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Bot, Send, User } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { PageHeader } from "@/components/PageHeader";
import { useAskAssistantMutation } from "@/services/api";
import { extractErrorMessage } from "@/lib/errors";
import { cn } from "@/lib/utils";
import type { AssistantResponse } from "@/types";

interface ChatMessage {
  role: "user" | "assistant";
  text: string;
  source?: AssistantResponse["source"];
}

/**
 * AssistantPage — "Ask SecureBank" chat UI over POST /assistant/ask.
 *
 * We keep the conversation in local component state (chat history is presentation,
 * not shared server data, so it doesn't belong in the RTK cache). Each question
 * fires the askAssistant mutation; the backend tags its reply as coming from the
 * LLM or the deterministic fallback, which we badge on the bubble.
 */
export function AssistantPage() {
  const { t } = useTranslation();
  const [ask, { isLoading }] = useAskAssistantMutation();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to the newest message.
  useEffect(() => {
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, isLoading]);

  async function submit(question: string) {
    const q = question.trim();
    if (!q || isLoading) return;
    setMessages((m) => [...m, { role: "user", text: q }]);
    setInput("");
    try {
      const res = await ask({ question: q }).unwrap();
      setMessages((m) => [
        ...m,
        { role: "assistant", text: res.answer, source: res.source },
      ]);
    } catch (err) {
      setMessages((m) => [
        ...m,
        {
          role: "assistant",
          text: extractErrorMessage(err as never, t("errors.generic")),
        },
      ]);
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    void submit(input);
  }

  const suggestions = [
    t("assistant.suggestions.s1"),
    t("assistant.suggestions.s2"),
    t("assistant.suggestions.s3"),
  ];

  return (
    <div className="mx-auto flex h-[calc(100vh-10rem)] max-w-3xl flex-col">
      <PageHeader title={t("assistant.title")} subtitle={t("assistant.subtitle")} />

      <Card className="flex flex-1 flex-col overflow-hidden">
        {/* Message list */}
        <div ref={scrollRef} className="flex-1 space-y-4 overflow-y-auto p-4">
          {messages.length === 0 && (
            <div className="flex h-full flex-col items-center justify-center gap-4 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
                <Bot className="h-7 w-7 text-primary" />
              </div>
              <p className="text-sm text-muted-foreground">
                {t("assistant.empty")}
              </p>
              <div className="flex flex-col items-center gap-2">
                <p className="text-xs font-medium text-muted-foreground">
                  {t("assistant.suggestions.label")}
                </p>
                <div className="flex flex-wrap justify-center gap-2">
                  {suggestions.map((s) => (
                    <Button
                      key={s}
                      variant="outline"
                      size="sm"
                      onClick={() => void submit(s)}
                    >
                      {s}
                    </Button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {messages.map((m, i) => (
            <div
              key={i}
              className={cn(
                "flex gap-3",
                m.role === "user" ? "flex-row-reverse" : "flex-row",
              )}
            >
              <div
                className={cn(
                  "flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
                  m.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-foreground",
                )}
              >
                {m.role === "user" ? (
                  <User className="h-4 w-4" />
                ) : (
                  <Bot className="h-4 w-4" />
                )}
              </div>
              <div
                className={cn(
                  "max-w-[80%] rounded-lg px-3 py-2 text-sm",
                  m.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted",
                )}
              >
                <p className="whitespace-pre-wrap">{m.text}</p>
                {m.role === "assistant" && m.source && (
                  <Badge variant="outline" className="mt-2">
                    {t("assistant.poweredBy")}{" "}
                    {m.source === "LLM"
                      ? t("assistant.sourceLLM")
                      : t("assistant.sourceFALLBACK")}
                  </Badge>
                )}
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="flex gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-muted">
                <Bot className="h-4 w-4" />
              </div>
              <div className="rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
                {t("assistant.thinking")}
              </div>
            </div>
          )}
        </div>

        {/* Composer */}
        <CardContent className="border-t p-3">
          <form onSubmit={onSubmit} className="flex gap-2">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={t("assistant.placeholder")}
              disabled={isLoading}
            />
            <Button type="submit" disabled={isLoading || !input.trim()}>
              <Send className="h-4 w-4" />
              <span className="hidden sm:inline">{t("assistant.send")}</span>
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
