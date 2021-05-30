#!/usr/bin/perl

# This script will look in your ~/.josm/plugins directory, create a new
# index.html, and upload both the jars and the index to the JOSM plugin
# server.

sub addJar
{
    my ($jar) = @_;
    system("rm -rf META-INF");
    system("unzip ~/Library/JOSM/plugins/${jar} META-INF/MANIFEST.MF > /dev/null");
    print OUT "${jar};http://josm-plugins.mypna.com/plugin/jars/${jar}";
    open(FILE, 'META-INF/MANIFEST.MF') or die "Can't read manifest\n";  
    while (<FILE>)
    {
    	my $startsWithSpace = /^ /;
    	$_ =~ s/[ \t\r\n]+$//;
    	$_ =~ s/^[ \t\r\n]+//;
    	if (!($_ eq ""))
    	{
    	    if ($startsWithSpace)
    	    {
    		print OUT;
    	    }
    	    else 
    	    {
    		print OUT "\n\t$_";
    	    }
    	}
    }
    print OUT "\n";
    close (FILE); 
    system("rm -rf META-INF");
}

sub installJar
{
    my ($jar) = @_;
	system("scp ~/Library/JOSM/plugins/${jar} josm-plugins.mypna.com:/var/www/html/plugin/jars");
}

sub installIndex
{
	mkdir('target');
	open(OUT, '>target/index.html') || die;
	
	addJar('telenav-geojson.jar');
	addJar('telenav-graph.jar');
	
	close OUT;
	
	system('scp target/index.html josm-plugins.mypna.com:/var/www/html/plugin');
}

chdir("$ENV{TDK_JOSM_PLUGINS_HOME}");

installIndex();
installJar('telenav-geojson.jar');
installJar('telenav-graph.jar');
