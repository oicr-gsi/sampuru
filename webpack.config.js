const HtmlWebpackPlugin = require("html-webpack-plugin")
const MiniCssExtractPlugin = require("mini-css-extract-plugin")

module.exports = {
  // https://webpack.js.org/concepts/entry-points/#multi-page-application
  entry: {
    index: './sampuru-ui/src/index.ts',
    project: './sampuru-ui/src/project.ts',
    qcables: './sampuru-ui/src/qcables.ts',
    cases: './sampuru-ui/src/cases.ts'
  },
  mode: "development",
  // https://webpack.js.org/configuration/dev-server/
  devServer: {
    open: true,
    host: 'local-ip',
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        secure: false,
        changeOrigin: true
      }
    },
    headers: {
      "Access-Control-Allow-Origin": "*",
    }
  },
  // https://webpack.js.org/concepts/loaders/
  module: {
    rules: [
      {
        test: /\.ts$/,
        use: 'ts-loader',
        exclude: /node_modules/
      },
      {
        test: /\.css$/,
        use: [
          MiniCssExtractPlugin.loader,
          {
            loader: "css-loader"
          }
        ]
      }
    ]
  },
  resolve: {
    extensions: ['.js', '.ts']
  },
  // https://webpack.js.org/concepts/plugins/
  plugins: [
    new HtmlWebpackPlugin({
      template: "sampuru-server/src/main/resources/static/index.html",
      inject: false,
      chunks: ['index'],
      filename: "index.html"
    }),
    new HtmlWebpackPlugin({
      template: "sampuru-server/src/main/resources/static/project.html",
      inject: false,
      chunks: ['project'],
      filename: "project.html"
    }),
    new HtmlWebpackPlugin({
      template: "sampuru-server/src/main/resources/static/qcables.html",
      inject: false,
      chunks: ['qcables'],
      filename: "qcables.html"
    }),
    new HtmlWebpackPlugin({
      template: "sampuru-server/src/main/resources/static/cases.html",
      inject: false,
      chunks: ['cases'],
      filename: "cases.html"
    }),
    new MiniCssExtractPlugin({
      filename: 'main.css',
      chunkFilename: "[id].css"
    })
  ]
}