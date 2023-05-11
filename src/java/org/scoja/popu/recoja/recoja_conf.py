# -*- coding: latin-1 -*-
# This file defines the Recoja configuration micro-language.
# It should be loaded in the Python interpreter before the
# Recoja configuration file.
#
# This code suppose that var "recoja.recoverer"
# contains a reference to a Recoverer.

import types
import java.text
import org.scoja.recoja

#======================================================================
# Separators
atInit = org.scoja.recoja.AtInitLocator()

def literal(str):
    return org.scoja.recoja.LiteralLocator(str)

def jump(n):
    return org.scoja.recoja.JumpLocator(n)


#======================================================================
# Formats
def format(eventEnd, dateEnd, date, dateStart = atInit):
    return org.scoja.recoja.Format(
        eventEnd, dateStart, dateEnd, date)

standardFormat = format(
    eventEnd = literal("\n"),
    dateEnd = jump(len("Jan 21 07:18:55")),
    date = "MMM dd HH:mm:ss")

anyother = None

def parse(files = anyother, withformat = standardFormat):
    recoja.recoverer.parseWith(org.scoja.recoja.FormatRule(files, withformat))


#======================================================================
# Clusters
def cluster(when, reduce):
    rew = org.scoja.recoja.RewritingRule(when)
    for a in reduce:
        if type(a) == types.IntType:
            rew.drop(a)
        elif type(a) == types.TupleType:
            rew.substitute(a[0], org.scoja.util.Substitution(a[1]))
#        else:
#            error "cluster second argument (reduce) should be an integer or a pair (integer,substitution)"
    recoja.recoverer.cluster(rew)

