use strict;
use warnings;

foreach my $file (glob("*.dot")) {
    open my $info, $file or die "Could not open $file: $!";
    $file =~ s/\.dot/_trans.dot/;
    open(my $fh, '>', $file) or die "Could not open file '$file' $!";
    
    my $startNext = 0;
    my $start = "";
    while( my $line = <$info>)  { 
        chomp($line);
        $line =~ s/^(s\d+) \[label="s\d+"\];$/$1 [shape="circle" label="$1"];/g;
        if($line =~ /^(s\d+)$/ or $line =~ /^label=""$/){
            next;
        }
        #$line =~ s/^(\d+) -> (\d+)/s$1 -> s$2/g;
        #$line =~ s/(\[label=".*)\/(.*"\])/$1 \/ $2;/g;
        $line =~ s/<<table border="0" cellpadding="1" cellspacing="0"><tr><td>/"/;
        $line =~ s/<\/td><td>\/<\/td><td>/\//;
        $line =~ s/<\/td><\/tr><\/table>>\]/"];/;
        if($line =~ /^}$/){
            print $fh "__start0 -> $start;";
            print $fh "\n";
        }        
        if($line =~ /(s\d+) \[color="red"\]/){
            print $fh '__start0 [label="" shape="none"];';
            print $fh "\n";
            $start = $1;
        }
        else {
            print $fh $line . "\n";
        }
    }
}
