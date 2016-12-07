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
        if($startNext == 1){
            $start = $line;
            $startNext = 0;
        }  
        $line =~ s/^(\d+)$/s$1 [shape="circle" label="s$1"];/g;
        $line =~ s/^(\d+) -> (\d+)/s$1 -> s$2/g;
        $line =~ s/(\[label=".*)\/(.*"\])/$1 \/ $2;/g;
        
        if($line =~ /^}$/){
            print $fh "__start0 -> s$start;";
            print $fh "\n";
        }    
        print $fh $line . "\n";    
        if($line =~ /digraph/){
            print $fh '__start0 [label="" shape="none"];';
            print $fh "\n";
            $startNext = 1;
        }
        
    }
}
