module.exports = {
  parserPreset: {
    parserOpts: {
      headerPattern: /^([a-z]+)\((\d+)\): (.+)$/,
      headerCorrespondence: ["type", "ticket", "subject"],
    },
  },
  rules: {
    "type-enum": [2, "always", ["feat", "fix", "chore", "docs", "style", "refactor", "perf", "test"]],
    "header-pattern": [2, "always", "^([a-z]+)\\(\\d+\\): .+"],
  },
};
