#!/usr/bin/env python

import sys
import os

"""
Usage:

./filter_fasta.py [path to input dir] [path to accepted taxon list]

Input files are expected to be in fasta format. The script will traverse all files
in the input dir, so the input dir should contain only fasta files. The taxon list
should be a line-delimited text file containing the names of tips as they
correspond to those in the fasta alignments."""

def filter(ifname, ofname, taxa_approved):

	tempofname = ofname + ".temp"
	infile = open(ifname, "rb")
	outfile = open(tempofname, "wb")
	
	ntax = 0
	OnValidTaxon = False

	firstline = True
	for line in infile:
		if line[0] == '>':
			cur_taxon = line.split()[0].strip('_')
			if cur_taxon.strip('>') in taxa_approved:
				outfile.write(cur_taxon + "\n")
				ntax += 1
				OnValidTaxon = True
			else:
				OnValidTaxon = False
		elif OnValidTaxon:
			outfile.write(line)			
	
	outfile.close()
		
	if ntax == 0:
		os.remove(tempofname)
	else:
		os.rename(tempofname,ofname)

	return ntax

ipath = sys.argv[1]

if os.path.isdir(ipath):
	usedir = True
	
	if ipath[len(ipath) - 1] != '/':
		ipath += '/'
	
	taxlistpath = sys.argv[2]
	taxlist = open(taxlistpath, "rU")
	taxa_approved = [taxon.strip() for taxon in taxlist.readlines()]

	ifiles = os.listdir(ipath)
	for ifname in ifiles:
		if ifname[0] != '.':
		
			ofname = ifname + "." + "filtered"
			
			print "\nfiltering " + ifname + " > " + ifname + ".filtered"
			
			ntaxfound = filter(ipath + ifname, ipath + ofname, taxa_approved)
			
			if ntaxfound:
				print "Retained " + str(ntaxfound) + " taxa."
			else:
				print "No acceptable taxa found. No output saved."

else:

	print "single case not yet implemented"