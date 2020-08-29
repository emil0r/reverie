// we use craco to theme antd
const path = require("path");
const CracoLessPlugin = require('craco-less');

module.exports = {
  plugins: [
    {
      plugin: CracoLessPlugin,
      options: {
        lessLoaderOptions: {
          lessOptions: {
              modifyVars: {
                  hack: `true; @import "${path.resolve(
                  __dirname,
                  "styling/theme.less"
                )}";`
              },
            javascriptEnabled: true,
          },
        },
      },
    },
  ],
};
