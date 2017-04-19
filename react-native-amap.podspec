require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "react-native-amap"
  s.version      = package['version']
  s.summary      = "React Native 下的高德地图组件"

  s.authors      = { "lgzhuo" => "lgzhuo@126.com" }
  s.homepage     = "https://github.com/lgzhuo/react-native-amap"
  s.license      = "MIT"
  s.platform     = :ios, "8.0"

  s.source       = { :git => "https://github.com/lgzhuo/react-native-amap.git" }
  s.source_files = "ios/RCTAMap/**/*.{h,m}"

  s.dependency 'React'
  s.dependency 'AMapNavi-NO-IDFA'
  s.dependency 'AMapLocation-NO-IDFA'
end
