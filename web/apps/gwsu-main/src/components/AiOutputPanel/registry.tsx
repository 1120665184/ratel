import { defineRegistry } from '@json-render/react';
import { catalog } from './catalog';
import Dashboard from './components/Dashboard';
import Section from './components/Section';
import StatCard from './components/StatCard';
import Chart from './components/Chart';
import DataTable from './components/DataTable';
import TextBlock from './components/TextBlock';
import ImageGallery from './components/ImageGallery';
import FlowChart from './components/FlowChart';

export const { registry } = defineRegistry(catalog, {
  components: {
    Dashboard,
    Section,
    StatCard,
    Chart,
    DataTable,
    TextBlock,
    ImageGallery,
    FlowChart,
  },
});
