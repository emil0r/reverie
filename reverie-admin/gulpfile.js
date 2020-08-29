// gulp and sass is used for custom styling
// antd is very opinionated and ties the styling
// very tightly together with react
// this makes it very hard (impossible?) to use it
// together with clojurescript

const through2 = require('through2');
const gulp = require('gulp');
const sass = require('gulp-sass');
const plumber = require("gulp-plumber");
const sourcemaps = require('gulp-sourcemaps');
const autoprefixer = require("gulp-autoprefixer");
const csso = require("gulp-csso");
var Fiber = require("fibers");
const log = require('fancy-log'); // instead of gulp-utils

sass.compiler = require('sass');  // using dart sass for latest features

const paths = {
  admin: {
    src: './styling/admin.scss',
    dest: "./public/css",
  }
};

function build_sass_admin() {
  return gulp
    .src(paths.admin.src)
    .pipe(plumber({
      errorHandler: function (err) {
        console.log(err.message);
        this.emit('end');
      }
    }))
    .pipe(sourcemaps.init())
    .pipe(sass({
      fiber: Fiber,
      outputStyle: 'compressed'
    }))
    .pipe(autoprefixer({
      cascade: false
    }))
    .pipe(csso({
      restructure: true,
      sourceMap: true,
      debug: false
    }))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.admin.dest));
}



function watch_admin() {
  gulp.watch('./styling/**/*.scss', build_sass_admin);
}

exports.default = build_sass_admin;
exports.watch = watch_admin;

exports.build_admin = build_sass_admin;
