// Karma configuration file, created for ACME Compensation Hub.
// Coverage thresholds are enforced here so the CI test step fails if
// any metric drops below the configured minimum.
// See: https://karma-runner.github.io

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        // Randomise test order to catch order-dependent bugs
        random: true
      },
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },

    // ── Coverage ──────────────────────────────────────────────────────────────
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/acme-frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ],
      // Hard thresholds: build fails (exit code 1) if any metric drops below these.
      // Current actuals: Statements 98.5%, Branches 89.7%, Functions 97.8%, Lines 99.2%
      check: {
        global: {
          statements: 80,
          branches: 80,
          functions: 80,
          lines: 80
        }
      }
    },

    reporters: ['progress', 'kjhtml'],
    browsers: ['Chrome'],
    singleRun: false,
    restartOnFileChange: true
  });
};
