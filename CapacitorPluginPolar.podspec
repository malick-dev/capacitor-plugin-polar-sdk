
  Pod::Spec.new do |s|
    s.name = 'CapacitorPluginPolar'
    s.version = '0.0.1'
    s.summary = 'HNL Capacitor plugin Polar'
    s.license = 'MIT'
    s.homepage = 'git@gitlab.com:heartneverlies/lena/teckpkg/capacitor-plugin-polar.git'
    s.author = 'NOC'
    s.source = { :git => 'git@gitlab.com:heartneverlies/lena/teckpkg/capacitor-plugin-polar.git', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end