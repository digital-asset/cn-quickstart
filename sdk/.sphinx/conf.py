# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), 'exts')))

def setup(sphinx):
    from pygments_daml_lexer import DamlLexer
    from ref import IgnoreRefRole, BrokenRefRole
    sphinx.add_lexer("daml", DamlLexer)
    sphinx.add_role('externalref', IgnoreRefRole())
    sphinx.add_role('subsiteref', IgnoreRefRole())
    sphinx.add_role('brokenref', IgnoreRefRole())

extensions = [
    'sphinxcontrib.mermaid',
    'sphinxcontrib.youtube',
    'wip'
]

source_suffix = '.rst'
