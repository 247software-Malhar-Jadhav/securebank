// PostCSS pipeline: Tailwind generates utility classes, autoprefixer adds
// vendor prefixes for older browsers. Standard shadcn/Tailwind setup.
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
