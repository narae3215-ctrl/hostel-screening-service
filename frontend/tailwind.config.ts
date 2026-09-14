import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        // docs/wireframe.html 블루프린트 팔레트와 통일
        ink: '#16324f',
        accent: '#2f6690',
        'accent-strong': '#103a5c',
        ok: '#2f7d3c',
        warn: '#9a6b08',
        review: '#5b6670',
        danger: '#a13d2f',
      },
    },
  },
  plugins: [],
};

export default config;
