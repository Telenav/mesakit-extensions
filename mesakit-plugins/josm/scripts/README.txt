
Building

It is IMPORTANT that all plugins are built at the same time using the build-tdk-josm-plugins.sh
script. If plugins are built independently, they may link to different versions of the 
TDK, which may cause hard to understand random failures since JOSM does not load plugins 
with a class loader!

Plugin Server

     Location: http://josm-plugins.mypna.com/
        Login: <username>@josm-plugins.mypna.com / ******
  Apache Home: (unknown on this server ... typically /usr/local/apache2.2)
Document Path: /var/www/html


