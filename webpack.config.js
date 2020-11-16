const path = require("path")
const HtmlWebpackPlugin = require("html-webpack-plugin")
const MiniCssExtractPlugin = require("mini-css-extract-plugin")

module.exports = {
  entry: './sampuru-ui/src/index.ts',
  mode: "development",
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'index.js'
  },
  devServer: {
    open: true,
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
  module: {
    rules: [
      {
        test: /\.ts$/,
        use: 'ts-loader',
        exclude: /node_modules/
      },
      {
        test: /\.scss$/,
        use: [
          MiniCssExtractPlugin.loader,
          {
            loader: "css-loader",
            options: {
              sourceMap: true,
              importLoader: 2
            }
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true
            }
          },
          'import-glob-loader'
        ]
      }
    ]
  },
  resolve: {
    extensions: ['.js', '.ts']
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: "sampuru-server/src/main/resources/static/index.html",
      inject: false
    }),
    new MiniCssExtractPlugin({
      filename: 'index.css',
      chunkFilename: "[id].css"
    })
  ]
}