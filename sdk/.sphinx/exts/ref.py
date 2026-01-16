# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

from docutils import nodes
from sphinx.util.docutils import SphinxRole

'''
Uses the ref as normal text:
- If it has text + label, use text. Example: :brokenref:`party <com.daml.ledger.api.v1.Commands.party>` -> party
- If it has label only, use label. Example: :brokenref:`daml-ecosystem-overview` -> daml-ecosystem-overview
'''
class BrokenRefRole(SphinxRole):
    def run(self):
        text = self.text

        label_start_index = text.find('<')

        if label_start_index == -1:
            node = nodes.inline(text=text.strip())
        else:
            node = nodes.inline(text=text[:label_start_index].strip())

        return [node], []

class IgnoreRefRole(SphinxRole):
    def run(self):
        node = nodes.literal(text=self.rawtext)
        return [node], []
