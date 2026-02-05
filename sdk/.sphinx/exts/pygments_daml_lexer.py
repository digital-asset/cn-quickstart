# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

from pygments.lexers.haskell import HaskellLexer
from pygments.lexer import inherit
from pygments.token import *

'''
Copy-pasted multiple times between repositories

TODO: Find a good way to reuse it without a hassle

Without it, sphinx generates the following warning:
    WARNING: Pygments lexer name 'daml' is not known

The alternative could be ignoring the warning, but that could let other warnings sneak in in the future
'''
class DamlLexer(HaskellLexer):

    name = 'Daml'
    aliases = ['daml']
    filenames = ['*.daml']

    daml_reserved = (
        'agreement',
        'can',
        'choice',
        'controller',
        'daml',
        'ensure',
        'exception',
        'for',
        'interface',
        'key',
        'maintainer',
        'message',
        'nonconsuming',
        'observer',
        'postconsuming',
        'preconsuming',
        'return',
        'signatory',
        'template',
        'this',
        'viewtype',
        'with',
    )

    tokens = {
        'root': [
            (r'\b(%s)(?!\')\b' % '|'.join(daml_reserved), Keyword.Reserved),
            (r'\b(True|False)\b', Keyword.Constant),
            (r'≡', Operator),
            inherit
        ]
    }
