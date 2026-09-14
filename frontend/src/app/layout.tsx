import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: '호스텔 용도변경 스크리닝',
  description: '건축물대장 기반 숙박시설 용도변경 1차 스크리닝 — 최종 인허가 판단이 아닙니다.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
