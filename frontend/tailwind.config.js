module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        arqly: {
          50: "rgb(var(--arqly-50) / <alpha-value>)",
          100: "rgb(var(--arqly-100) / <alpha-value>)",
          200: "rgb(var(--arqly-200) / <alpha-value>)",
          500: "rgb(var(--arqly-500) / <alpha-value>)",
          600: "rgb(var(--arqly-600) / <alpha-value>)",
          700: "rgb(var(--arqly-700) / <alpha-value>)",
          900: "rgb(var(--arqly-900) / <alpha-value>)"
        }
      },
      boxShadow: {
        soft: "0 18px 60px rgba(15, 23, 42, 0.08)"
      }
    }
  },
  plugins: []
};
